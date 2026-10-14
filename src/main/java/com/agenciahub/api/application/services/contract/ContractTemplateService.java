package com.agenciahub.api.application.services.contract;

import com.agenciahub.api.domain.contract.ContractField;
import com.agenciahub.api.domain.contract.ContractTemplateRenderer;
import com.agenciahub.api.domain.contract.Contracts.*;
import com.agenciahub.api.exception.ContractConflictException;
import com.agenciahub.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Modelos de contrato fornecidos pela agência, com versões imutáveis do conteúdo. */
@Service
@RequiredArgsConstructor
public class ContractTemplateService {

    static final String TEMPLATE = "TEMPLATE";

    private final JdbcTemplate jdbc;
    private final ContractSupport support;

    private static final String SUMMARY_SQL = """
            SELECT t.*, (SELECT count(*) FROM contracts c WHERE c.template_id = t.id) AS contract_count
            FROM contract_templates t WHERE t.agency_id = ?""";

    private static final RowMapper<TemplateSummary> SUMMARY = (rs, n) -> new TemplateSummary(
            rs.getObject("id", UUID.class), Product.valueOf(rs.getString("product")), rs.getString("name"), rs.getString("description"),
            rs.getInt("current_version"), rs.getBoolean("archived"), rs.getInt("row_version"),
            rs.getLong("contract_count"), rs.getObject("updated_at", OffsetDateTime.class));

    public List<FieldView> fields() {
        return Arrays.stream(ContractField.values()).map(FieldView::of).toList();
    }

    @Transactional(readOnly = true)
    public List<TemplateSummary> list(boolean includeArchived, Product product) {
        String sql = SUMMARY_SQL + (includeArchived ? "" : " AND NOT t.archived") + (product == null ? "" : " AND t.product = ?")
                + " ORDER BY t.archived, lower(t.name), t.id";
        return product == null ? jdbc.query(sql, SUMMARY, support.agency()) : jdbc.query(sql, SUMMARY, support.agency(), product.name());
    }

    @Transactional(readOnly = true)
    public TemplateDetail get(UUID id) {
        TemplateSummary template = summary(id, false);
        List<TemplateVersion> versions = versions(id);
        TemplateVersion current = versions.stream().filter(v -> v.version() == template.currentVersion()).findFirst()
                .orElseThrow(() -> new IllegalStateException("Versão atual do modelo não encontrada."));
        return new TemplateDetail(template, current.body(), current.usedFields(), versions, support.history(id));
    }

    @Transactional(readOnly = true)
    public TemplateVersion version(UUID id, int version) {
        summary(id, false);
        return versions(id).stream().filter(v -> v.version() == version).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Versão do modelo não encontrada."));
    }

    @Transactional
    public TemplateDetail create(TemplateInput input) {
        String body = validated(input);
        if (input.product() == null) throw new IllegalArgumentException("Informe se o modelo é de Viagens ou de Milhas.");
        UUID id = UUID.randomUUID();
        String author = support.author();
        jdbc.update("INSERT INTO contract_templates(id, agency_id, product, name, description, created_by) VALUES (?, ?, ?, ?, ?, ?)",
                id, support.agency(), input.product().name(), input.name().strip(), blankToNull(input.description()), author);
        insertVersion(id, 1, body, author);
        support.audit(TEMPLATE, id, null, "TEMPLATE_CREATED", auditDetail(input.product(), input.name(), 1));
        return get(id);
    }

    /** Edita nome/descrição no mesmo registro; conteúdo alterado vira nova versão, preservando as anteriores. */
    @Transactional
    public TemplateDetail update(UUID id, TemplateInput input) {
        TemplateSummary before = summary(id, true);
        if (before.archived()) throw new ContractConflictException("Reative o modelo antes de editá-lo.");
        support.expectVersion(before.rowVersion(), input.expectedVersion());
        if (input.product() != null && input.product() != before.product()) {
            throw new IllegalArgumentException("O produto do modelo não pode ser alterado. Crie um novo modelo para o outro produto.");
        }
        String body = validated(input);
        String currentBody = version(id, before.currentVersion()).body();
        boolean bodyChanged = !currentBody.equals(body);
        int nextVersion = bodyChanged ? before.currentVersion() + 1 : before.currentVersion();
        if (bodyChanged) insertVersion(id, nextVersion, body, support.author());
        jdbc.update("""
                UPDATE contract_templates SET name = ?, description = ?, current_version = ?, row_version = row_version + 1,
                updated_at = now() WHERE id = ? AND agency_id = ?""",
                input.name().strip(), blankToNull(input.description()), nextVersion, id, support.agency());
        support.audit(TEMPLATE, id, null, bodyChanged ? "TEMPLATE_VERSIONED" : "TEMPLATE_UPDATED", auditDetail(before.product(), input.name(), nextVersion));
        return get(id);
    }

    /** Exclui modelos sem contratos; modelos já usados são arquivados para preservar os documentos emitidos. */
    @Transactional
    public DeleteOutcome delete(UUID id, Integer expectedVersion) {
        TemplateSummary before = summary(id, true);
        support.expectVersion(before.rowVersion(), expectedVersion);
        if (before.contractCount() == 0) {
            jdbc.update("DELETE FROM contract_templates WHERE id = ? AND agency_id = ?", id, support.agency());
            support.audit(TEMPLATE, id, null, "TEMPLATE_DELETED", auditDetail(before.product(), before.name(), before.currentVersion()));
            return new DeleteOutcome("DELETED", "Modelo excluído.");
        }
        if (!before.archived()) {
            jdbc.update("UPDATE contract_templates SET archived = true, row_version = row_version + 1, updated_at = now() WHERE id = ? AND agency_id = ?",
                    id, support.agency());
            support.audit(TEMPLATE, id, null, "TEMPLATE_ARCHIVED", auditDetail(before.product(), before.name(), before.currentVersion()));
        }
        return new DeleteOutcome("ARCHIVED",
                "O modelo já foi usado em contratos e foi arquivado para preservar o histórico. Ele não aparece para novos rascunhos.");
    }

    @Transactional
    public TemplateDetail restore(UUID id, Integer expectedVersion) {
        TemplateSummary before = summary(id, true);
        if (!before.archived()) return get(id);
        support.expectVersion(before.rowVersion(), expectedVersion);
        jdbc.update("UPDATE contract_templates SET archived = false, row_version = row_version + 1, updated_at = now() WHERE id = ? AND agency_id = ?",
                id, support.agency());
        support.audit(TEMPLATE, id, null, "TEMPLATE_RESTORED", auditDetail(before.product(), before.name(), before.currentVersion()));
        return get(id);
    }

    TemplateSummary summary(UUID id, boolean lock) {
        if (lock) {
            // Bloqueia o modelo para serializar edições, versões e exclusão.
            List<UUID> locked = jdbc.query("SELECT id FROM contract_templates WHERE id = ? AND agency_id = ? FOR UPDATE",
                    (rs, n) -> rs.getObject(1, UUID.class), id, support.agency());
            if (locked.isEmpty()) throw notFound();
        }
        return jdbc.query(SUMMARY_SQL + " AND t.id = ?", SUMMARY, support.agency(), id).stream().findFirst()
                .orElseThrow(ContractTemplateService::notFound);
    }

    String body(UUID templateId, int version) {
        return jdbc.query("SELECT body FROM contract_template_versions WHERE template_id = ? AND agency_id = ? AND version = ?",
                        (rs, n) -> rs.getString(1), templateId, support.agency(), version).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Versão do modelo não encontrada."));
    }

    private List<TemplateVersion> versions(UUID id) {
        return jdbc.query("""
                SELECT version, body, author, created_at FROM contract_template_versions
                WHERE template_id = ? AND agency_id = ? ORDER BY version DESC""",
                (rs, n) -> {
                    String body = rs.getString("body");
                    return new TemplateVersion(rs.getInt("version"), body, usedFields(body), rs.getString("author"),
                            rs.getObject("created_at", OffsetDateTime.class));
                }, id, support.agency());
    }

    private void insertVersion(UUID templateId, int version, String body, String author) {
        jdbc.update("INSERT INTO contract_template_versions(id, template_id, agency_id, version, body, author) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), templateId, support.agency(), version, body, author);
    }

    private static String validated(TemplateInput input) {
        if (input.name() == null || input.name().isBlank()) throw new IllegalArgumentException("Informe o nome do modelo.");
        ContractTemplateRenderer.validate(input.body());
        return input.body();
    }

    private static List<String> usedFields(String body) {
        return ContractTemplateRenderer.validate(body).stream().map(ContractField::key).toList();
    }

    private static Map<String, Object> auditDetail(Product product, String name, int version) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("product", product.name());
        detail.put("name", name == null ? null : name.strip());
        detail.put("templateVersion", version);
        return detail;
    }

    static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.strip(); }

    private static ResourceNotFoundException notFound() { return new ResourceNotFoundException("Modelo de contrato não encontrado."); }
}
