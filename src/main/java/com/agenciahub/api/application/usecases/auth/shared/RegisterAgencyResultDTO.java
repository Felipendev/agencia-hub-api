package com.agenciahub.api.application.usecases.auth.shared;

import java.util.UUID;

public record RegisterAgencyResultDTO(
        UUID agencyId,
        UUID userId,
        String message
) {
}
