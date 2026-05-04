package com.agenciahub.api.dto.customer;

import com.agenciahub.api.domain.CustomerStatus;

/**
 * Partial update: {@code null} fields are ignored (no change).
 */
public record UpdateCustomerRequest(
        String name,
        String email,
        String phone,
        String interestDestination,
        CustomerStatus status,
        String notes
) {
}
