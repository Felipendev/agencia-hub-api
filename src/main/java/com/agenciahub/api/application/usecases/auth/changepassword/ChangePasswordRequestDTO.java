package com.agenciahub.api.application.usecases.auth.changepassword;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDTO(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8) String newPassword,
        @NotBlank String newPasswordConfirmation
) {
}
