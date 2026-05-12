package com.agenciahub.api.dto.quotation;

import com.agenciahub.api.domain.QuotationStatus;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Partial update: {@code null} fields are ignored.
 */
public record UpdateQuotationRequest(
        UUID opportunityId,
        UUID sellerId,
        String title,
        String destination,
        String description,
        BigDecimal totalAmount,
        String currency,
        QuotationStatus status,
        LocalDate validUntil,
        LocalDate travelStartDate,
        LocalDate travelEndDate,
        JsonNode details,
        List<String> tags,
        Boolean priority,
        String assignee,
        String internalNotes,
        Boolean unsetSeller
) {
}
