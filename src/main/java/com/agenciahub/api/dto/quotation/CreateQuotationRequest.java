package com.agenciahub.api.dto.quotation;

import com.agenciahub.api.domain.QuotationCreationSource;
import com.agenciahub.api.domain.QuotationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Mirrors the SPA quotation flow: summary fields here, full form payload in {@code details}
 * (English keys — same shape as the Next.js {@code CotacaoDetalhes} document).
 */
public record CreateQuotationRequest(
        @NotNull UUID customerId,
        UUID opportunityId,
        UUID sellerId,
        @NotBlank String title,
        @NotBlank String destination,
        String description,
        @NotNull @PositiveOrZero BigDecimal totalAmount,
        String currency,
        QuotationStatus status,
        @NotNull LocalDate validUntil,
        LocalDate travelStartDate,
        LocalDate travelEndDate,
        @Schema(description = "Structured fields from the multi-step quotation form (JSON object).")
        JsonNode details,
        List<String> tags,
        Boolean priority,
        String assignee,
        @Schema(description = "Internal team notes (not shown to the traveller).")
        String internalNotes,
        @Schema(description = "INTERNAL (default) or PUBLIC_FORM when imported from public link.")
        QuotationCreationSource creationSource,
        @Schema(description = "ID of solicitacao_submissions row when created from public form import.")
        UUID publicSubmissionId
) {
}
