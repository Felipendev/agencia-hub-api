package com.agenciahub.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterViaInviteRequest(
        @NotBlank String token,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String passwordConfirmation,
        String phone
) {
}
