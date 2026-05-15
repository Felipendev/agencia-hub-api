package com.agenciahub.api.application.usecases.auth.validatetoken;

public record InviteValidationResponseDTO(
        String email,
        String agencyName
) {
}
