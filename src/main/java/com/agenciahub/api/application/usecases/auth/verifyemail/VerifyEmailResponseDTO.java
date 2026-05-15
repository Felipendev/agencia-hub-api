package com.agenciahub.api.application.usecases.auth.verifyemail;

import com.agenciahub.api.domain.enums.AccountKind;

import java.util.UUID;

public record VerifyEmailResponseDTO(
        String token,
        UUID userId,
        String name,
        String email,
        AccountKind accountKind,
        UUID agencyId,
        String agencyName,
        String publicLinkCode
) {
}
