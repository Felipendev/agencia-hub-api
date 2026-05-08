package com.agenciahub.api.dto.auth;

public record InviteValidationResponse(
        String email,
        String agencyName
) {
}
