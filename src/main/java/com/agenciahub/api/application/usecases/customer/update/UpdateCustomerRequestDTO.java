package com.agenciahub.api.application.usecases.customer.update;

import com.agenciahub.api.domain.CustomerStatus;

/**
 * Partial update: {@code null} fields are ignored (no change).
 */
public record UpdateCustomerRequestDTO(
        String name,
        String email,
        String phone,
        String interestDestination,
        CustomerStatus status,
        String notes
) {
}
