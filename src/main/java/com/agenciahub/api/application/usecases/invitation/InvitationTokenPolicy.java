package com.agenciahub.api.application.usecases.invitation;

import com.agenciahub.api.domain.InvitationStatus;
import com.agenciahub.api.entity.Invitation;

import java.time.Instant;

public final class InvitationTokenPolicy {

    private InvitationTokenPolicy() {}

    public static void ensureUsable(Invitation invitation) {
        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            throw new IllegalStateException("este convite já foi utilizado");
        }

        if (invitation.getStatus() == InvitationStatus.REVOKED) {
            throw new IllegalStateException("este convite foi cancelado");
        }

        if (Instant.now().isAfter(invitation.getExpiresAt())) {
            throw new IllegalStateException("este convite expirou");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("este convite não está mais disponível");
        }
    }
}
