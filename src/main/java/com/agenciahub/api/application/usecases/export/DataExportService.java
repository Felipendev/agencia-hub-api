package com.agenciahub.api.application.usecases.export;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.application.persistence.entity.FinancialEntry;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.entity.Quotation;
import com.agenciahub.api.application.persistence.entity.SolicitacaoSubmission;
import com.agenciahub.api.application.persistence.entity.ConsentimentoLog;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.ConsentimentoLogRepository;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.persistence.repository.FinancialEntryRepository;
import com.agenciahub.api.application.persistence.repository.QuotationRepository;
import com.agenciahub.api.application.persistence.repository.SolicitacaoSubmissionRepository;
import com.agenciahub.api.domain.enums.AccountKind;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class DataExportService {

    private final AgencyRepository agencyRepository;
    private final CrmCustomerRepository customerRepository;
    private final QuotationRepository quotationRepository;
    private final FinancialEntryRepository financialEntryRepository;
    private final SolicitacaoSubmissionRepository submissionRepository;
    private final ConsentimentoLogRepository consentimentoLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public byte[] buildOwnerExport(PlatformAccount account) throws IOException {
        UUID agencyId = account.getAgency() != null ? account.getAgency().getId() : null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            addEntry(zos, "account.json", buildAccountNode(account));
            if (agencyId != null) {
                Agency agency = agencyRepository.findById(agencyId).orElse(null);
                if (agency != null) {
                    addEntry(zos, "agency.json", buildAgencyNode(agency));
                }
                addEntry(zos, "customers.json",
                        buildArrayNode(customerRepository.findByAgency_Id(agencyId), this::buildCustomerNode));
                addEntry(zos, "quotations.json",
                        buildArrayNode(quotationRepository.findByAgency_Id(agencyId), this::buildQuotationNode));
                addEntry(zos, "financial_entries.json",
                        buildArrayNode(financialEntryRepository.findByAgency_Id(agencyId), this::buildFinancialEntryNode));
                addEntry(zos, "submissions.json",
                        buildArrayNode(submissionRepository.findByAgency_IdOrderByCreatedAtDesc(agencyId), this::buildSubmissionNode));
                addEntry(zos, "consents.json",
                        buildArrayNode(consentimentoLogRepository.findByAgencyId(agencyId), this::buildConsentNode));
            }
        }
        return baos.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] buildAgentExport(PlatformAccount account) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            addEntry(zos, "account.json", buildAccountNode(account));
            List<Quotation> quotations = quotationRepository.findByAgency_Id(
                    account.getAgency() != null ? account.getAgency().getId() : UUID.randomUUID()
            ).stream().filter(q -> q.getSeller() != null && q.getSeller().getId().equals(account.getId())).toList();
            addEntry(zos, "quotations_assigned.json",
                    buildArrayNode(quotations, this::buildQuotationNode));
            addEntry(zos, "commissions.json",
                    buildArrayNode(quotations.stream()
                                    .filter(q -> q.getStatus() != null && q.getStatus().name().equals("ACCEPTED"))
                                    .toList(),
                            this::buildCommissionNode));
        }
        return baos.toByteArray();
    }

    public boolean isOwner(PlatformAccount account) {
        return account.getAccountKind() == AccountKind.AGENCY_OWNER;
    }

    // ── Node builders ──────────────────────────────────────────────────────────

    private ObjectNode buildAccountNode(PlatformAccount a) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("id", a.getId().toString());
        n.put("name", a.getName());
        n.put("email", a.getEmail());
        n.put("accountKind", a.getAccountKind().name());
        n.put("active", Boolean.TRUE.equals(a.getActive()));
        if (a.getPhone() != null) n.put("phone", a.getPhone());
        if (a.getCreatedAt() != null) n.put("createdAt", a.getCreatedAt().toString());
        return n;
    }

    private ObjectNode buildAgencyNode(Agency a) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("id", a.getId().toString());
        n.put("name", a.getName());
        if (a.getCommercialEmail() != null) n.put("commercialEmail", a.getCommercialEmail());
        if (a.getPhone() != null) n.put("phone", a.getPhone());
        if (a.getCnpj() != null) n.put("cnpj", a.getCnpj());
        if (a.getAddress() != null) n.put("address", a.getAddress());
        n.put("status", a.getStatus().name());
        if (a.getCreatedAt() != null) n.put("createdAt", a.getCreatedAt().toString());
        return n;
    }

    private ObjectNode buildCustomerNode(CrmCustomer c) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("id", c.getId().toString());
        n.put("name", c.getName());
        n.put("email", c.getEmail());
        n.put("phone", c.getPhone());
        n.put("interestDestination", c.getInterestDestination());
        n.put("status", c.getStatus().name());
        if (c.getNotes() != null) n.put("notes", c.getNotes());
        if (c.getCreatedAt() != null) n.put("createdAt", c.getCreatedAt().toString());
        return n;
    }

    private ObjectNode buildQuotationNode(Quotation q) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("id", q.getId().toString());
        n.put("title", q.getTitle());
        n.put("destination", q.getDestination());
        n.put("status", q.getStatus().name());
        n.put("totalAmount", q.getTotalAmount().toPlainString());
        n.put("currency", q.getCurrency());
        if (q.getValidUntil() != null) n.put("validUntil", q.getValidUntil().toString());
        if (q.getTravelStartDate() != null) n.put("travelStartDate", q.getTravelStartDate().toString());
        if (q.getTravelEndDate() != null) n.put("travelEndDate", q.getTravelEndDate().toString());
        if (q.getCustomer() != null) n.put("customerId", q.getCustomer().getId().toString());
        if (q.getSeller() != null) n.put("sellerId", q.getSeller().getId().toString());
        if (q.getCreatedAt() != null) n.put("createdAt", q.getCreatedAt().toString());
        return n;
    }

    private ObjectNode buildFinancialEntryNode(FinancialEntry f) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("id", f.getId().toString());
        n.put("description", f.getDescription());
        n.put("type", f.getType().name());
        n.put("category", f.getCategory().name());
        n.put("amount", f.getAmount().toPlainString());
        n.put("status", f.getStatus().name());
        if (f.getEntryDate() != null) n.put("entryDate", f.getEntryDate().toString());
        if (f.getCustomer() != null) n.put("customerId", f.getCustomer().getId().toString());
        return n;
    }

    private ObjectNode buildSubmissionNode(SolicitacaoSubmission s) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("id", s.getId().toString());
        n.put("nome", s.getNome());
        n.put("email", s.getEmail());
        n.put("telefone", s.getTelefone());
        n.put("consentimentoLgpd", s.isConsentimentoLgpd());
        if (s.getConsentimentoAt() != null) n.put("consentimentoAt", s.getConsentimentoAt().toString());
        if (s.getConsentimentoVersaoTermos() != null) n.put("consentimentoVersaoTermos", s.getConsentimentoVersaoTermos());
        if (s.getCreatedAt() != null) n.put("createdAt", s.getCreatedAt().toString());
        return n;
    }

    private ObjectNode buildConsentNode(ConsentimentoLog c) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("id", c.getId().toString());
        if (c.getSubmission() != null) n.put("submissionId", c.getSubmission().getId().toString());
        n.put("tipo", c.getTipo());
        if (c.getIp() != null) n.put("ip", c.getIp());
        if (c.getVersaoTermos() != null) n.put("versaoTermos", c.getVersaoTermos());
        if (c.getCreatedAt() != null) n.put("createdAt", c.getCreatedAt().toString());
        return n;
    }

    private ObjectNode buildCommissionNode(Quotation q) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("quotationId", q.getId().toString());
        n.put("title", q.getTitle());
        n.put("totalAmount", q.getTotalAmount().toPlainString());
        n.put("status", q.getStatus().name());
        if (q.getCreatedAt() != null) n.put("createdAt", q.getCreatedAt().toString());
        return n;
    }

    // ── ZIP helpers ───────────────────────────────────────────────────────────

    private <T> ArrayNode buildArrayNode(List<T> items, java.util.function.Function<T, ObjectNode> mapper) {
        ArrayNode arr = objectMapper.createArrayNode();
        items.forEach(item -> arr.add(mapper.apply(item)));
        return arr;
    }

    private void addEntry(ZipOutputStream zos, String entryName, Object data) throws IOException {
        byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(bytes);
        zos.closeEntry();
    }
}
