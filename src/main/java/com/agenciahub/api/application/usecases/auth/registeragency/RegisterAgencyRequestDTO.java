package com.agenciahub.api.application.usecases.auth.registeragency;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterAgencyRequestDTO(
        @NotBlank @Size(max = 255) String agencyName,
        @NotBlank @Size(max = 255) String ownerName,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String passwordConfirmation,
        @NotBlank String ownerPhone,
        String agencyPhone,
        @NotNull Boolean termsAccepted,
        @NotBlank String termsVersion
) {
}
