package com.agenciahub.api.application.usecases.invitation.revokeinvitation;

import java.util.UUID;

public record RevokeInvitationCommand(UUID invitationId, UUID agencyId) {
}
