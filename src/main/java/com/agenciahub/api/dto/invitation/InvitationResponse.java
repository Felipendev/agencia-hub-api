package com.agenciahub.api.dto.invitation;

import com.agenciahub.api.domain.InvitationStatus;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        String email,
        String token,
        String inviteUrl,
        InvitationStatus status,
        Instant expiresAt,
        Instant createdAt
) {
}
