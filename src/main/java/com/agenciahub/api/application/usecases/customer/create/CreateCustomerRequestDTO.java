package com.agenciahub.api.application.usecases.customer.create;

import com.agenciahub.api.domain.CustomerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequestDTO(
        @NotBlank String name,
        @Email String email,
        String phone,
        @Size(max = 512) String interestDestination,
        @NotNull CustomerStatus status,
        String notes
) {
}
