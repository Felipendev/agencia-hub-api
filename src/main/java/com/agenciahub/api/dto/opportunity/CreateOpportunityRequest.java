package com.agenciahub.api.dto.opportunity;

import com.agenciahub.api.domain.OpportunityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateOpportunityRequest(
        @NotNull UUID customerId,
        @NotBlank String title,
        @NotBlank String destination,
        @NotNull BigDecimal estimatedAmount,
        @NotNull OpportunityStatus status,
        @NotNull LocalDate expectedTravelDate,
        String notes
) {
}
