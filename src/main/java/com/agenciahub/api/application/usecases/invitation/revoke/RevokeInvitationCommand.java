package com.agenciahub.api.application.usecases.invitation.revoke;

import java.util.UUID;

public record RevokeInvitationCommand(UUID invitationId, UUID agencyId) {
}
