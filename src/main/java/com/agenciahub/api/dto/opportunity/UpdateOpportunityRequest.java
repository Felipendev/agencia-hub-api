package com.agenciahub.api.dto.opportunity;

import com.agenciahub.api.domain.OpportunityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Partial update: {@code null} fields are ignored.
 */
public record UpdateOpportunityRequest(
        String title,
        String destination,
        BigDecimal estimatedAmount,
        OpportunityStatus status,
        LocalDate expectedTravelDate,
        String notes
) {
}
