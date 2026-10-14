package com.agenciahub.api.domain.contract;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/** Contratos de entrada e saída do módulo F02. */
public final class Contracts {
    private Contracts() {}

    public static final ZoneId AGENCY_ZONE = ZoneId.of("America/Fortaleza");

    public static LocalDate today() { return LocalDate.now(AGENCY_ZONE); }

    /** Produto do relacionamento: nunca deduzido do cliente nem do uso de milhas numa viagem. */
    public enum Product { TRAVEL, MILES }

    public enum Status { DRAFT, SENT, SIGNED, CANCELLED }

    public enum PaymentMode { CASH, INSTALLMENTS }

    public enum AttachmentKind { SIGNED_EVIDENCE, SUPPORTING }

    // ---- Modelos ----

    public record TemplateInput(
            /* Obrigatório na criação; não pode ser alterado depois. */ Product product,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 1000) String description,
            @NotBlank @Size(max = ContractTemplateRenderer.MAX_BODY_LENGTH) String body,
            @Min(0) Integer expectedVersion) {}

    public record TemplateSummary(UUID id, Product product, String name, String description, int currentVersion, boolean archived,
                                  int rowVersion, long contractCount, OffsetDateTime updatedAt) {}

    public record TemplateVersion(int version, String body, List<String> usedFields, String author, OffsetDateTime createdAt) {}

    public record TemplateDetail(TemplateSummary template, String body, List<String> usedFields,
                                 List<TemplateVersion> versions, List<AuditEntry> history) {}

    public record DeleteOutcome(String outcome, String message) {}

    public record FieldView(String key, String label, String source) {
        public static FieldView of(ContractField field) {
            return new FieldView(field.key(), field.label(), field.source().name());
        }
    }

    // ---- Contratos ----

    public record DraftInput(
            @NotNull UUID customerId,
            @NotNull Product product,
            @NotNull UUID templateId,
            @Min(1) Integer templateVersion,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 200) String planName,
            @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal price,
            LocalDate startDate,
            LocalDate endDate,
            PaymentMode paymentMode,
            @Min(1) @Max(120) Integer installments,
            @Size(max = 2000) String paymentTerms,
            @Size(max = 4000) String notes,
            @Min(0) Integer expectedVersion) {}

    public record VersionInput(@NotNull @Min(1) Integer expectedVersion) {}

    public record SendInput(@NotNull @Min(1) Integer expectedVersion, @NotNull LocalDate sentOn) {}

    public record ReasonInput(@NotNull @Min(1) Integer expectedVersion, @NotBlank @Size(max = 1000) String reason) {}

    public record ContractSummary(UUID id, Product product, UUID customerId, String customerName, UUID templateId, String templateName,
                                  int templateVersion, String title, Status status, String planName, BigDecimal price,
                                  LocalDate startDate, LocalDate endDate, Integer latestDocumentVersion,
                                  boolean documentCurrent, int rowVersion, OffsetDateTime updatedAt) {}

    public record DocumentMeta(int version, Product product, int dataRevision, UUID templateId, int templateVersion,
                               String contentSha256, String author, OffsetDateTime createdAt) {}

    public record DocumentView(DocumentMeta document, String renderedContent, Object dataSnapshot) {}

    public record AttachmentMeta(UUID id, AttachmentKind kind, Integer documentVersion, String description,
                                 String filename, String contentType, long byteSize, String author, OffsetDateTime createdAt) {}

    public record ReceivableLink(UUID receivableId, UUID saleId, int installmentNumber, BigDecimal amount,
                                 LocalDate dueDate, String status, String author, OffsetDateTime linkedAt) {}

    public record AuditEntry(UUID id, String action, Object detail, String author, OffsetDateTime createdAt) {}

    public record ContractDetail(
            ContractSummary contract,
            PaymentMode paymentMode,
            Integer installments,
            String paymentTerms,
            String notes,
            int dataRevision,
            int latestTemplateVersion,
            LocalDate sentOn,
            LocalDate signedOn,
            OffsetDateTime cancelledAt,
            String cancelReason,
            List<FieldView> missingFields,
            List<DocumentMeta> documents,
            List<AttachmentMeta> attachments,
            /* null quando o perfil não tem acesso ao financeiro */ List<ReceivableLink> receivables,
            List<AuditEntry> history) {}

    public record Preview(String content, List<FieldView> missingFields, int templateVersion, int dataRevision) {}

    public static void invalid(String message) { throw new IllegalArgumentException(message); }
}
