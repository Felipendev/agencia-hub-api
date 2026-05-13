package com.agenciahub.api.dto.auth;

import com.agenciahub.api.domain.UserRole;

import java.util.UUID;

public record VerifyEmailResponse(
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
