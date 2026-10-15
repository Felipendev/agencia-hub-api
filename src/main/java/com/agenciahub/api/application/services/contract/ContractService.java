package com.agenciahub.api.application.services.contract;

import com.agenciahub.api.domain.contract.ContractField;
import com.agenciahub.api.domain.contract.ContractTemplateRenderer;
import com.agenciahub.api.domain.contract.ContractTemplateRenderer.Rendered;
import com.agenciahub.api.domain.contract.ContractValues;
import com.agenciahub.api.domain.contract.Contracts;
import com.agenciahub.api.domain.contract.Contracts.*;
import com.agenciahub.api.exception.ContractConflictException;
import com.agenciahub.api.exception.ContractIncompleteException;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.agenciahub.api.domain.contract.Contracts.invalid;

/**
 * Ciclo do contrato: rascunho → documento emitido (retrato imutável) → enviado → assinado, com cancelamento.
 * Nunca cria venda, receita ou cobrança; recebíveis existentes só são vinculados explicitamente.
 */
@Service
@RequiredArgsConstructor
public class ContractService {

    static final String CONTRACT = "CONTRACT";
    static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    static final Set<String> FILE_TYPES = Set.of("application/pdf", "image/jpeg", "image/png", "image/webp",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final JdbcTemplate jdbc;
    private final ContractSupport support;
    private final ContractTemplateService templates;
    private final ContractPdfRenderer pdf;
    private final ObjectMapper mapper;

    /** Linha completa da tabela {@code contracts}. */
    record Row(UUID id, Product product, UUID customerId, String customerName, UUID templateId, String templateName, int templateVersion,
               int latestTemplateVersion, String title, String planName, BigDecimal price, LocalDate startDate,
               LocalDate endDate, PaymentMode paymentMode, Integer installments, String paymentTerms, String notes,
               Status status, int dataRevision, int rowVersion, LocalDate sentOn, LocalDate signedOn,
               OffsetDateTime cancelledAt, String cancelReason, OffsetDateTime updatedAt,
               Integer latestDocumentVersion, Integer latestDocumentRevision) {

        boolean documentCurrent() {
            return latestDocumentRevision != null && latestDocumentRevision == dataRevision;
        }

        ContractSummary summary() {
            return new ContractSummary(id, product, customerId, customerName, templateId, templateName, templateVersion, title, status,
                    planName, price, startDate, endDate, latestDocumentVersion, documentCurrent(), rowVersion, updatedAt);
        }

        ContractValues.Terms terms() {
            return new ContractValues.Terms(product, title, planName, price, startDate, endDate, paymentMode, installments, paymentTerms);
        }
    }

    private static final String ROW_SQL = """
            SELECT c.*, p.name AS customer_name, t.name AS template_name, t.current_version AS latest_template_version,
                   d.version AS latest_document_version, d.data_revision AS latest_document_revision
            FROM contracts c
            JOIN customers p ON p.id = c.customer_id
            JOIN contract_templates t ON t.id = c.template_id
            LEFT JOIN LATERAL (SELECT version, data_revision FROM contract_documents x
                               WHERE x.contract_id = c.id ORDER BY version DESC LIMIT 1) d ON true
            WHERE c.agency_id = ?""";

    private static final RowMapper<Row> ROW = (rs, n) -> new Row(
            rs.getObject("id", UUID.class), Product.valueOf(rs.getString("product")), rs.getObject("customer_id", UUID.class), rs.getString("customer_name"),
            rs.getObject("template_id", UUID.class), rs.getString("template_name"), rs.getInt("template_version"),
            rs.getInt("latest_template_version"), rs.getString("title"), rs.getString("plan_name"), rs.getBigDecimal("price"),
            rs.getObject("start_date", LocalDate.class), rs.getObject("end_date", LocalDate.class),
            Optional.ofNullable(rs.getString("payment_mode")).map(PaymentMode::valueOf).orElse(null),
            (Integer) rs.getObject("installments"), rs.getString("payment_terms"), rs.getString("notes"),
            Status.valueOf(rs.getString("status")), rs.getInt("data_revision"), rs.getInt("row_version"),
            rs.getObject("sent_on", LocalDate.class), rs.getObject("signed_on", LocalDate.class),
            rs.getObject("cancelled_at", OffsetDateTime.class), rs.getString("cancel_reason"),
            rs.getObject("updated_at", OffsetDateTime.class),
            (Integer) rs.getObject("latest_document_version"), (Integer) rs.getObject("latest_document_revision"));

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public List<ContractSummary> list(UUID customerId, Product product, Status status) {
        StringBuilder sql = new StringBuilder(ROW_SQL);
        List<Object> args = new ArrayList<>(List.of(support.agency()));
        if (product != null) { sql.append(" AND c.product = ?"); args.add(product.name()); }
        if (customerId != null) { sql.append(" AND c.customer_id = ?"); args.add(customerId); }
        if (status != null) { sql.append(" AND c.status = ?"); args.add(status.name()); }
        sql.append(" ORDER BY c.updated_at DESC, c.id");
        return jdbc.query(sql.toString(), ROW, args.toArray()).stream().map(Row::summary).toList();
    }

    @Transactional(readOnly = true)
    public ContractDetail detail(UUID id) {
        return detail(row(id, false));
    }

    @Transactional(readOnly = true)
    public Preview preview(UUID id) {
        Row row = row(id, false);
        Rendered rendered = render(row, Contracts.today());
        return new Preview(rendered.content(), fieldViews(rendered.missing()), row.templateVersion(), row.dataRevision());
    }

    @Transactional(readOnly = true)
    public DocumentView document(UUID id, int version) {
        row(id, false);
        return jdbc.query("SELECT * FROM contract_documents WHERE contract_id = ? AND agency_id = ? AND version = ?",
                        (rs, n) -> new DocumentView(documentMeta(rs), rs.getString("rendered_content"),
                                support.read(rs.getString("data_snapshot"))),
                        id, support.agency(), version).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado."));
    }

    public record FileContent(String filename, String contentType, byte[] bytes) {}

    @Transactional(readOnly = true)
    public FileContent documentPdf(UUID id, int version) {
        Row row = row(id, false);
        DocumentView view = document(id, version);
        DocumentMeta meta = view.document();
        String issued = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .format(meta.createdAt().atZoneSameInstant(Contracts.AGENCY_ZONE));
        String footer = "Documento v%d · Modelo v%d · Emitido em %s · SHA-256 %s"
                .formatted(meta.version(), meta.templateVersion(), issued, meta.contentSha256().substring(0, 16));
        JsonNode snapshot = mapper.valueToTree(view.dataSnapshot());
        String issuedTitle = snapshot.path("fields").path("contrato.titulo").asText(row.title());
        byte[] bytes = pdf.render(issuedTitle, view.renderedContent(), footer);
        return new FileContent(safeFilename(issuedTitle) + "-v" + meta.version() + ".pdf", "application/pdf", bytes);
    }

    @Transactional(readOnly = true)
    public FileContent attachment(UUID id, UUID attachmentId) {
        row(id, false);
        return jdbc.query("SELECT filename, content_type, content FROM contract_attachments WHERE id = ? AND contract_id = ? AND agency_id = ?",
                        (rs, n) -> new FileContent(rs.getString(1), rs.getString(2), rs.getBytes(3)),
                        attachmentId, id, support.agency()).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Anexo não encontrado."));
    }

    // ------------------------------------------------------------------ rascunho

    @Transactional
    public ContractDetail create(UUID requestKey, DraftInput input) {
        lockCustomer(input.customerId());
        Optional<Row> existing = find(requestKey);
        if (existing.isPresent()) {
            Row row = existing.get();
            if (!row.customerId().equals(input.customerId()) || row.product() != input.product() || !row.templateId().equals(input.templateId())
                    || !row.title().equals(input.title().strip())) {
                throw new ContractConflictException("A confirmação já foi utilizada para outro contrato.");
            }
            return detail(row);
        }
        Boolean deleted = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM contract_audit WHERE agency_id = ? AND resource_id = ? AND action = 'CONTRACT_DELETED')",
                Boolean.class, support.agency(), requestKey);
        if (Boolean.TRUE.equals(deleted)) {
            throw new ContractConflictException("Este rascunho já foi excluído. Recarregue a página antes de criar outro.");
        }
        int templateVersion = activeTemplateVersion(input.templateId(), input.templateVersion(), input.product());
        Terms terms = terms(input);
        jdbc.update("""
                INSERT INTO contracts(id, agency_id, product, customer_id, template_id, template_version, title, plan_name, price,
                    start_date, end_date, payment_mode, installments, payment_terms, notes, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                requestKey, support.agency(), input.product().name(), input.customerId(), input.templateId(), templateVersion, terms.title(),
                terms.planName(), terms.price(), terms.startDate(), terms.endDate(), name(terms.paymentMode()),
                terms.installments(), terms.paymentTerms(), terms.notes(), support.author());
        Row row = row(requestKey, false);
        support.audit(CONTRACT, row.id(), row.customerId(), "CONTRACT_CREATED", termsDetail(row));
        return detail(row);
    }

    @Transactional
    public ContractDetail update(UUID id, DraftInput input) {
        Row before = row(id, true);
        requireStatus(before, Status.DRAFT, "Somente rascunhos podem ser editados. Para alterar um contrato enviado, inicie uma revisão.");
        support.expectVersion(before.rowVersion(), input.expectedVersion());
        if (!before.customerId().equals(input.customerId())) {
            invalid("A pessoa do contrato não pode ser trocada. Crie um novo rascunho para outra pessoa.");
        }
        if (before.product() != input.product()) {
            invalid("O produto do contrato não pode ser trocado. Crie um novo contrato para o outro produto.");
        }
        boolean sameTemplate = before.templateId().equals(input.templateId());
        // Sem versão explícita, o rascunho mantém a versão do modelo que já usa (inclusive de modelo arquivado).
        Integer requested = input.templateVersion() != null ? input.templateVersion() : sameTemplate ? before.templateVersion() : null;
        int templateVersion = sameTemplate && requested != null && requested == before.templateVersion()
                ? before.templateVersion()
                : activeTemplateVersion(input.templateId(), requested, before.product());
        Terms terms = terms(input);
        boolean unchanged = before.templateId().equals(input.templateId()) && before.templateVersion() == templateVersion
                && terms.equals(new Terms(before.title(), before.planName(), before.price(), before.startDate(), before.endDate(),
                before.paymentMode(), before.installments(), before.paymentTerms(), before.notes()));
        if (unchanged) return detail(before);
        jdbc.update("""
                UPDATE contracts SET template_id = ?, template_version = ?, title = ?, plan_name = ?, price = ?, start_date = ?,
                    end_date = ?, payment_mode = ?, installments = ?, payment_terms = ?, notes = ?,
                    data_revision = data_revision + 1, row_version = row_version + 1, updated_at = now()
                WHERE id = ? AND agency_id = ?""",
                input.templateId(), templateVersion, terms.title(), terms.planName(), terms.price(), terms.startDate(),
                terms.endDate(), name(terms.paymentMode()), terms.installments(), terms.paymentTerms(), terms.notes(),
                id, support.agency());
        Row row = row(id, false);
        support.audit(CONTRACT, id, row.customerId(), "CONTRACT_UPDATED", termsDetail(row));
        return detail(row);
    }

    /** Exclusão definitiva somente de rascunhos sem documento emitido; emitidos usam cancelamento. */
    @Transactional
    public void delete(UUID id, Integer expectedVersion) {
        Row row = row(id, true);
        requireStatus(row, Status.DRAFT, "Contratos enviados ou assinados não podem ser excluídos. Use o cancelamento para preservar o histórico.");
        support.expectVersion(row.rowVersion(), expectedVersion);
        if (row.latestDocumentVersion() != null) {
            throw new ContractConflictException("Este rascunho já gerou documento. Cancele o contrato para preservar o documento emitido.");
        }
        jdbc.update("DELETE FROM contract_receivable_links WHERE contract_id = ? AND agency_id = ?", id, support.agency());
        jdbc.update("DELETE FROM contract_attachments WHERE contract_id = ? AND agency_id = ?", id, support.agency());
        jdbc.update("DELETE FROM contracts WHERE id = ? AND agency_id = ?", id, support.agency());
        support.audit(CONTRACT, id, row.customerId(), "CONTRACT_DELETED", termsDetail(row));
    }

    // ------------------------------------------------------------------ emissão e estados

    /**
     * Gera o documento com o retrato dos dados atuais. Repetir sem mudanças devolve o mesmo documento;
     * alterações posteriores na pessoa, agência ou modelo não alteram documentos já emitidos.
     */
    @Transactional
    public DocumentMeta generate(UUID id, Integer expectedVersion) {
        Row row = row(id, true);
        requireStatus(row, Status.DRAFT, "Inicie uma revisão para gerar uma nova versão do documento.");
        support.expectVersion(row.rowVersion(), expectedVersion);
        LocalDate issueDate = Contracts.today();
        Snapshot snapshot = snapshot(row, issueDate);
        Rendered rendered = ContractTemplateRenderer.render(snapshot.templateBody(), snapshot.values());
        if (!rendered.missing().isEmpty()) throw new ContractIncompleteException(fieldViews(rendered.missing()));
        String hash = sha256(rendered.content());

        Optional<DocumentMeta> latest = documents(id).stream().findFirst();
        if (latest.isPresent() && latest.get().contentSha256().equals(hash) && latest.get().dataRevision() == row.dataRevision()
                && latest.get().templateVersion() == row.templateVersion()) {
            return latest.get();
        }
        int version = latest.map(d -> d.version() + 1).orElse(1);
        jdbc.update("""
                INSERT INTO contract_documents(id, contract_id, agency_id, version, product, data_revision, template_id, template_version,
                    template_body, rendered_content, data_snapshot, content_sha256, author)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)""",
                UUID.randomUUID(), id, support.agency(), version, row.product().name(), row.dataRevision(), row.templateId(), row.templateVersion(),
                snapshot.templateBody(), rendered.content(), support.json(snapshot.data()), hash, support.author());
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("documentVersion", version);
        detail.put("templateVersion", row.templateVersion());
        detail.put("contentSha256", hash);
        support.audit(CONTRACT, id, row.customerId(), "DOCUMENT_GENERATED", detail);
        return documents(id).get(0);
    }

    @Transactional
    public ContractDetail send(UUID id, SendInput input) {
        Row row = row(id, true);
        requireStatus(row, Status.DRAFT, "Somente rascunhos com documento gerado podem ser marcados como enviados.");
        support.expectVersion(row.rowVersion(), input.expectedVersion());
        if (!row.documentCurrent()) {
            throw new ContractConflictException("Gere o documento com os dados atuais antes de marcar como enviado.");
        }
        if (input.sentOn().isAfter(Contracts.today())) invalid("A data de envio não pode ser futura.");
        transition(id, Status.SENT, "sent_on = ?", input.sentOn());
        support.audit(CONTRACT, id, row.customerId(), "CONTRACT_SENT",
                Map.of("documentVersion", row.latestDocumentVersion(), "sentOn", input.sentOn().toString()));
        return detail(id);
    }

    /** Reabre um contrato enviado como rascunho; o documento enviado permanece no histórico. */
    @Transactional
    public ContractDetail revise(UUID id, ReasonInput input) {
        Row row = row(id, true);
        requireStatus(row, Status.SENT, "Somente contratos enviados e ainda não assinados podem ser revisados.");
        support.expectVersion(row.rowVersion(), input.expectedVersion());
        transition(id, Status.DRAFT, "sent_on = NULL");
        support.audit(CONTRACT, id, row.customerId(), "REVISION_STARTED",
                Map.of("fromDocumentVersion", row.latestDocumentVersion(), "reason", input.reason().strip()));
        return detail(id);
    }

    /** Registra a assinatura com evidência anexada. Não é assinatura eletrônica integrada. */
    @Transactional
    public ContractDetail sign(UUID id, Integer expectedVersion, LocalDate signedOn, String note, MultipartFile evidence) {
        Row row = row(id, true);
        requireStatus(row, Status.SENT, "Marque o contrato como enviado antes de registrar a assinatura.");
        support.expectVersion(row.rowVersion(), expectedVersion);
        if (signedOn == null) invalid("Informe a data da assinatura.");
        if (signedOn.isAfter(Contracts.today())) invalid("A data da assinatura não pode ser futura.");
        if (row.sentOn() != null && signedOn.isBefore(row.sentOn())) invalid("A data da assinatura não pode ser anterior ao envio.");
        if (evidence == null || evidence.isEmpty()) invalid("Anexe o documento assinado ou outra evidência da assinatura.");
        UUID attachmentId = storeAttachment(id, AttachmentKind.SIGNED_EVIDENCE, row.latestDocumentVersion(), note, evidence);
        transition(id, Status.SIGNED, "signed_on = ?", signedOn);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("documentVersion", row.latestDocumentVersion());
        detail.put("signedOn", signedOn.toString());
        detail.put("evidenceId", attachmentId);
        detail.put("evidenceFilename", filename(evidence));
        detail.put("method", "EVIDENCE_ATTACHED");
        support.audit(CONTRACT, id, row.customerId(), "CONTRACT_SIGNED", detail);
        return detail(id);
    }

    @Transactional
    public ContractDetail cancel(UUID id, ReasonInput input) {
        Row row = row(id, true);
        if (row.status() == Status.CANCELLED) return detail(row);
        support.expectVersion(row.rowVersion(), input.expectedVersion());
        if (row.status() != Status.DRAFT && !support.isOwner()) {
            throw new AccessDeniedException("Somente o dono da agência pode cancelar contratos enviados ou assinados.");
        }
        transition(id, Status.CANCELLED, "cancelled_at = now(), cancel_reason = ?", input.reason().strip());
        support.audit(CONTRACT, id, row.customerId(), "CONTRACT_CANCELLED",
                Map.of("fromStatus", row.status().name(), "reason", input.reason().strip()));
        return detail(id);
    }

    // ------------------------------------------------------------------ anexos

    @Transactional
    public AttachmentMeta addAttachment(UUID id, String description, MultipartFile file) {
        Row row = row(id, true);
        if (row.status() == Status.CANCELLED) throw new ContractConflictException("Contratos cancelados não recebem novos anexos.");
        UUID attachmentId = storeAttachment(id, AttachmentKind.SUPPORTING, null, description, file);
        support.audit(CONTRACT, id, row.customerId(), "ATTACHMENT_ADDED", Map.of("attachmentId", attachmentId, "filename", filename(file)));
        return attachments(id).stream().filter(a -> a.id().equals(attachmentId)).findFirst().orElseThrow();
    }

    @Transactional
    public void deleteAttachment(UUID id, UUID attachmentId) {
        Row row = row(id, true);
        AttachmentMeta attachment = attachments(id).stream().filter(a -> a.id().equals(attachmentId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Anexo não encontrado."));
        if (attachment.kind() == AttachmentKind.SIGNED_EVIDENCE) {
            throw new ContractConflictException("A evidência de assinatura faz parte do histórico e não pode ser excluída. Cancele o contrato se necessário.");
        }
        if (row.status() == Status.CANCELLED) throw new ContractConflictException("Anexos de contratos cancelados são preservados.");
        jdbc.update("DELETE FROM contract_attachments WHERE id = ? AND contract_id = ? AND agency_id = ?", attachmentId, id, support.agency());
        support.audit(CONTRACT, id, row.customerId(), "ATTACHMENT_DELETED", Map.of("attachmentId", attachmentId, "filename", attachment.filename()));
    }

    // ------------------------------------------------------------------ recebíveis existentes

    @Transactional
    public ContractDetail linkReceivable(UUID id, UUID receivableId) {
        requireOwner();
        Row row = row(id, true);
        if (row.status() == Status.CANCELLED) throw new ContractConflictException("Contratos cancelados não recebem novos vínculos.");
        Boolean valid = jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM receivables r JOIN sales s ON s.id = r.sale_id
                              WHERE r.id = ? AND s.agency_id = ? AND s.customer_id = ?)""",
                Boolean.class, receivableId, support.agency(), row.customerId());
        if (!Boolean.TRUE.equals(valid)) throw new ResourceNotFoundException("Recebível não encontrado para esta pessoa.");
        int inserted = jdbc.update("""
                INSERT INTO contract_receivable_links(contract_id, receivable_id, agency_id, author) VALUES (?, ?, ?, ?)
                ON CONFLICT DO NOTHING""", id, receivableId, support.agency(), support.author());
        if (inserted == 1) support.audit(CONTRACT, id, row.customerId(), "RECEIVABLE_LINKED", Map.of("receivableId", receivableId));
        return detail(id);
    }

    @Transactional
    public ContractDetail unlinkReceivable(UUID id, UUID receivableId) {
        requireOwner();
        Row row = row(id, true);
        int removed = jdbc.update("DELETE FROM contract_receivable_links WHERE contract_id = ? AND receivable_id = ? AND agency_id = ?",
                id, receivableId, support.agency());
        if (removed == 1) support.audit(CONTRACT, id, row.customerId(), "RECEIVABLE_UNLINKED", Map.of("receivableId", receivableId));
        return detail(id);
    }

    // ------------------------------------------------------------------ apoio

    /** Termos normalizados e validados de um rascunho. */
    record Terms(String title, String planName, BigDecimal price, LocalDate startDate, LocalDate endDate,
                 PaymentMode paymentMode, Integer installments, String paymentTerms, String notes) {}

    private static Terms terms(DraftInput input) {
        if (input.startDate() != null && input.endDate() != null && input.endDate().isBefore(input.startDate())) {
            invalid("O fim da vigência não pode ser anterior ao início.");
        }
        Integer installments = input.installments();
        if (input.paymentMode() == PaymentMode.INSTALLMENTS && (installments == null || installments < 2)) {
            invalid("Informe ao menos 2 parcelas para pagamento parcelado.");
        }
        if (input.paymentMode() != PaymentMode.INSTALLMENTS) installments = null;
        BigDecimal price = input.price() == null ? null : input.price().setScale(2, java.math.RoundingMode.UNNECESSARY);
        return new Terms(input.title().strip(), ContractTemplateService.blankToNull(input.planName()), price,
                input.startDate(), input.endDate(), input.paymentMode(), installments,
                ContractTemplateService.blankToNull(input.paymentTerms()), ContractTemplateService.blankToNull(input.notes()));
    }

    private int activeTemplateVersion(UUID templateId, Integer requested, Product product) {
        record TemplateState(boolean archived, Product product) {}
        // FOR SHARE impede a exclusão concorrente do modelo enquanto o rascunho é gravado.
        List<TemplateState> found = jdbc.query("SELECT archived, product FROM contract_templates WHERE id = ? AND agency_id = ? FOR SHARE",
                (rs, n) -> new TemplateState(rs.getBoolean(1), Product.valueOf(rs.getString(2))), templateId, support.agency());
        if (found.isEmpty()) throw new ResourceNotFoundException("Modelo de contrato não encontrado.");
        if (found.get(0).product() != product) {
            invalid("O modelo escolhido é de outro produto. Use um modelo de " + (product == Product.TRAVEL ? "Viagens." : "Milhas."));
        }
        if (found.get(0).archived()) throw new ContractConflictException("Este modelo está arquivado. Reative-o ou escolha outro modelo.");
        int version = requested != null ? requested : templates.summary(templateId, false).currentVersion();
        templates.body(templateId, version);
        return version;
    }

    private void lockCustomer(UUID customerId) {
        List<UUID> found = jdbc.query("SELECT id FROM customers WHERE id = ? AND agency_id = ? FOR UPDATE",
                (rs, n) -> rs.getObject(1, UUID.class), customerId, support.agency());
        if (found.isEmpty()) throw new ResourceNotFoundException("Pessoa não encontrada.");
    }

    private Optional<Row> find(UUID id) {
        return jdbc.query(ROW_SQL + " AND c.id = ?", ROW, support.agency(), id).stream().findFirst();
    }

    private Row row(UUID id, boolean lock) {
        if (lock) {
            List<UUID> locked = jdbc.query("SELECT id FROM contracts WHERE id = ? AND agency_id = ? FOR UPDATE",
                    (rs, n) -> rs.getObject(1, UUID.class), id, support.agency());
            if (locked.isEmpty()) throw notFound();
        }
        return find(id).orElseThrow(ContractService::notFound);
    }

    private void transition(UUID id, Status target, String setClause, Object... args) {
        List<Object> params = new ArrayList<>(List.of(target.name()));
        params.addAll(Arrays.asList(args));
        params.add(id);
        params.add(support.agency());
        jdbc.update("UPDATE contracts SET status = ?, " + setClause + ", row_version = row_version + 1, updated_at = now() WHERE id = ? AND agency_id = ?",
                params.toArray());
    }

    private static void requireStatus(Row row, Status expected, String message) {
        if (row.status() != expected) throw new ContractConflictException(message);
    }

    private void requireOwner() {
        if (!support.isOwner()) throw new AccessDeniedException("Somente o dono da agência pode vincular recebíveis.");
    }

    record Snapshot(String templateBody, Map<ContractField, String> values, Map<String, Object> data) {}

    private Snapshot snapshot(Row row, LocalDate issueDate) {
        ContractValues.Customer customer = jdbc.query("SELECT name, email, phone, profile_data FROM customers WHERE id = ? AND agency_id = ?",
                        (rs, n) -> new ContractValues.Customer(rs.getString("name"), rs.getString("email"), rs.getString("phone"),
                                readTree(rs.getString("profile_data"))),
                        row.customerId(), support.agency()).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada."));
        ContractValues.Agency agency = jdbc.queryForObject("SELECT name, cnpj, address, commercial_email, phone FROM agencies WHERE id = ?",
                (rs, n) -> new ContractValues.Agency(rs.getString("name"), rs.getString("cnpj"), rs.getString("address"),
                        rs.getString("commercial_email"), rs.getString("phone")),
                support.agency());
        String body = templates.body(row.templateId(), row.templateVersion());
        Map<ContractField, String> values = ContractValues.resolve(customer, agency, row.terms(), issueDate);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("issueDate", issueDate.toString());
        data.put("customerId", row.customerId());
        data.put("product", row.product().name());
        data.put("templateId", row.templateId());
        data.put("templateName", row.templateName());
        data.put("templateVersion", row.templateVersion());
        data.put("dataRevision", row.dataRevision());
        Map<String, String> fields = new LinkedHashMap<>();
        values.forEach((field, value) -> fields.put(field.key(), value));
        data.put("fields", fields);
        return new Snapshot(body, values, data);
    }

    private Rendered render(Row row, LocalDate issueDate) {
        Snapshot snapshot = snapshot(row, issueDate);
        return ContractTemplateRenderer.render(snapshot.templateBody(), snapshot.values());
    }

    private ContractDetail detail(Row row) {
        Rendered rendered = row.status() == Status.DRAFT ? render(row, Contracts.today()) : null;
        List<FieldView> missing = rendered == null ? List.of() : fieldViews(rendered.missing());
        return new ContractDetail(row.summary(), row.paymentMode(), row.installments(), row.paymentTerms(), row.notes(),
                row.dataRevision(), row.latestTemplateVersion(), row.sentOn(), row.signedOn(), row.cancelledAt(),
                row.cancelReason(), missing, documents(row.id()), attachments(row.id()),
                support.isOwner() ? receivables(row.id()) : null, support.history(row.id()));
    }


    private List<DocumentMeta> documents(UUID id) {
        return jdbc.query("""
                SELECT version, product, data_revision, template_id, template_version, content_sha256, author, created_at
                FROM contract_documents WHERE contract_id = ? AND agency_id = ? ORDER BY version DESC""",
                (rs, n) -> documentMeta(rs), id, support.agency());
    }

    private static DocumentMeta documentMeta(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new DocumentMeta(rs.getInt("version"), Product.valueOf(rs.getString("product")), rs.getInt("data_revision"), rs.getObject("template_id", UUID.class),
                rs.getInt("template_version"), rs.getString("content_sha256"), rs.getString("author"),
                rs.getObject("created_at", OffsetDateTime.class));
    }

    private List<AttachmentMeta> attachments(UUID id) {
        return jdbc.query("""
                SELECT id, kind, document_version, description, filename, content_type, byte_size, author, created_at
                FROM contract_attachments WHERE contract_id = ? AND agency_id = ? ORDER BY created_at, id""",
                (rs, n) -> new AttachmentMeta(rs.getObject("id", UUID.class), AttachmentKind.valueOf(rs.getString("kind")),
                        (Integer) rs.getObject("document_version"), rs.getString("description"), rs.getString("filename"),
                        rs.getString("content_type"), rs.getLong("byte_size"), rs.getString("author"),
                        rs.getObject("created_at", OffsetDateTime.class)),
                id, support.agency());
    }

    private List<ReceivableLink> receivables(UUID id) {
        return jdbc.query("""
                SELECT r.id, r.sale_id, r.installment_number, r.amount, r.due_date, r.status, l.author, l.created_at
                FROM contract_receivable_links l JOIN receivables r ON r.id = l.receivable_id
                WHERE l.contract_id = ? AND l.agency_id = ? ORDER BY r.due_date, r.installment_number""",
                (rs, n) -> new ReceivableLink(rs.getObject("id", UUID.class), rs.getObject("sale_id", UUID.class),
                        rs.getInt("installment_number"), rs.getBigDecimal("amount"), rs.getObject("due_date", LocalDate.class),
                        rs.getString("status"), rs.getString("author"), rs.getObject("created_at", OffsetDateTime.class)),
                id, support.agency());
    }

    private UUID storeAttachment(UUID contractId, AttachmentKind kind, Integer documentVersion, String description, MultipartFile file) {
        if (file == null || file.isEmpty()) invalid("Escolha um arquivo.");
        if (file.getSize() > MAX_FILE_BYTES) invalid("O arquivo pode ter até 5 MB.");
        String type = Objects.toString(file.getContentType(), "");
        if (!FILE_TYPES.contains(type)) invalid("Envie PDF, imagem (JPG, PNG, WEBP) ou documento Word.");
        if (description != null && description.length() > 1000) invalid("A descrição pode ter até 1.000 caracteres.");
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("""
                    INSERT INTO contract_attachments(id, contract_id, agency_id, kind, document_version, description, filename,
                        content_type, byte_size, content, author)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    id, contractId, support.agency(), kind.name(), documentVersion, ContractTemplateService.blankToNull(description),
                    filename(file), type, file.getSize(), file.getBytes(), support.author());
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler o arquivo enviado.", e);
        }
        return id;
    }

    private static String filename(MultipartFile file) {
        String name = Objects.toString(file.getOriginalFilename(), "arquivo").replaceAll("[\\r\\n\\\\/]", "_").strip();
        if (name.isEmpty()) name = "arquivo";
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    static String safeFilename(String title) {
        String normalized = java.text.Normalizer.normalize(title, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.isEmpty()) slug = "contrato";
        return slug.length() > 80 ? slug.substring(0, 80) : slug;
    }

    private static List<FieldView> fieldViews(List<ContractField> fields) {
        return fields.stream().map(FieldView::of).toList();
    }

    private Map<String, Object> termsDetail(Row row) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("product", row.product().name());
        detail.put("title", row.title());
        detail.put("templateId", row.templateId());
        detail.put("templateVersion", row.templateVersion());
        detail.put("planName", row.planName());
        detail.put("price", row.price());
        detail.put("startDate", Objects.toString(row.startDate(), null));
        detail.put("endDate", Objects.toString(row.endDate(), null));
        detail.put("paymentMode", name(row.paymentMode()));
        detail.put("installments", row.installments());
        detail.put("dataRevision", row.dataRevision());
        return detail;
    }

    private JsonNode readTree(String json) {
        try {
            return json == null ? null : mapper.readTree(json);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler o cadastro da pessoa.", e);
        }
    }

    static String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String name(Enum<?> value) { return value == null ? null : value.name(); }

    private static ResourceNotFoundException notFound() { return new ResourceNotFoundException("Contrato não encontrado."); }
}
