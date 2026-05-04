package com.agenciahub.api.dto.customer;

import com.agenciahub.api.domain.CustomerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCustomerRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String phone,
        @NotBlank String interestDestination,
        @NotNull CustomerStatus status,
        String notes
) {
}
