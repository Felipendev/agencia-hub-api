package com.agenciahub.api.application.usecases.auth.forgotpassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequestDTO(
        @NotBlank @Email String email
) {
}
