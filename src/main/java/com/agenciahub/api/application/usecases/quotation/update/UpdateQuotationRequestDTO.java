package com.agenciahub.api.application.usecases.quotation.update;

import com.agenciahub.api.domain.QuotationStatus;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Partial update: {@code null} fields are ignored.
 */
public record UpdateQuotationRequestDTO(
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
        Boolean unsetSeller,
        JsonNode flightPlan
) {
    /** Compatibility constructor for callers without a flight plan. */
    public UpdateQuotationRequestDTO(UUID sellerId, String title, String destination, String description, BigDecimal totalAmount, String currency, QuotationStatus status, LocalDate validUntil, LocalDate travelStartDate, LocalDate travelEndDate, JsonNode details, List<String> tags, Boolean priority, String assignee, String internalNotes, Boolean unsetSeller) {
        this(sellerId, title, destination, description, totalAmount, currency, status, validUntil, travelStartDate, travelEndDate, details, tags, priority, assignee, internalNotes, unsetSeller, null);
    }
}
