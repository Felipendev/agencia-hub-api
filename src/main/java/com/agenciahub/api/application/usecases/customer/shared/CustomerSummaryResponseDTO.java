package com.agenciahub.api.application.usecases.customer.shared;

import com.agenciahub.api.domain.CustomerStatus;

import java.time.Instant;
import java.util.UUID;

public record CustomerSummaryResponseDTO(
        UUID id,
        String name,
        String email,
        String phone,
        String interestDestination,
        CustomerStatus status,
        String notes,
        Instant createdAt
) {
}
