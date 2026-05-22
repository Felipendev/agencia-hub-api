package com.agenciahub.api.application.usecases.solicitacao.pub.consent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeConsentRequestDTO(
        @NotBlank @Size(max = 320) String email,
        @NotBlank @Size(max = 32) String telefone
) {
}
