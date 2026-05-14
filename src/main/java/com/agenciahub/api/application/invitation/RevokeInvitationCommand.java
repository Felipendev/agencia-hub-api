package com.agenciahub.api.application.invitation;

import java.util.UUID;

public record RevokeInvitationCommand(UUID invitationId, UUID agencyId) {
}
