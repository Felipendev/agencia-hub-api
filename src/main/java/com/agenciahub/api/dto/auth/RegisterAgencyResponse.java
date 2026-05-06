package com.agenciahub.api.dto.auth;

import java.util.UUID;

public record RegisterAgencyResponse(
        UUID agencyId,
        UUID userId,
        String message
) {
}
