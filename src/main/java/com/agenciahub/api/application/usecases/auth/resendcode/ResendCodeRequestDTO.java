package com.agenciahub.api.application.usecases.auth.resendcode;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendCodeRequestDTO(
        @NotBlank @Email String email
) {
}
