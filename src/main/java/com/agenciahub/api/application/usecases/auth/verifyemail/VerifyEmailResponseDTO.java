package com.agenciahub.api.application.usecases.auth.verifyemail;

import com.agenciahub.api.domain.UserRole;

import java.util.UUID;

public record VerifyEmailResponseDTO(
        String token,
        UUID userId,
        String name,
        String email,
        UserRole role,
        UUID agencyId,
        String agencyName,
        String publicLinkCode
) {
}
