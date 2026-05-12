package com.agenciahub.api.dto.quotation;

import com.agenciahub.api.domain.QuotationCreationSource;
import com.agenciahub.api.domain.QuotationStatus;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QuotationResponse(
        UUID id,
        UUID customerId,
        String customerName,
        UUID opportunityId,
        String opportunityTitle,
        UUID sellerId,
        String sellerName,
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
        boolean priority,
        String assignee,
        String internalNotes,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        QuotationCreationSource creationSource,
        UUID createdByUserId,
        String createdByUserName,
        UUID publicSubmissionId
) {
}
