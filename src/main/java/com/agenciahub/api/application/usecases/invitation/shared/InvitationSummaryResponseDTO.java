package com.agenciahub.api.application.usecases.invitation.shared;

import com.agenciahub.api.domain.InvitationStatus;

import java.time.Instant;
import java.util.UUID;

public record InvitationSummaryResponseDTO(
        UUID id,
        String email,
        String token,
        String inviteUrl,
        InvitationStatus status,
        Instant expiresAt,
        Instant createdAt
) {
}
