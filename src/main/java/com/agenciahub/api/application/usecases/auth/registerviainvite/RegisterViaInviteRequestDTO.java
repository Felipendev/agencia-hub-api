package com.agenciahub.api.application.usecases.auth.registerviainvite;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterViaInviteRequestDTO(
        @NotBlank String token,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String passwordConfirmation,
        String phone
) {
}
