package com.agenciahub.api.dto.customer;

import com.agenciahub.api.domain.CustomerStatus;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String interestDestination,
        CustomerStatus status,
        String notes,
        Instant createdAt,
        Instant deletedAt
) {
}
