package com.agenciahub.api.dto.opportunity;

import com.agenciahub.api.domain.OpportunityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OpportunityResponse(
        UUID id,
        UUID customerId,
        String customerName,
        String title,
        String destination,
        BigDecimal estimatedAmount,
        OpportunityStatus status,
        LocalDate expectedTravelDate,
        String notes
) {
}
