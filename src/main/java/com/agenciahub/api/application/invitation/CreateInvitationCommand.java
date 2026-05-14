package com.agenciahub.api.application.invitation;

import com.agenciahub.api.dto.invitation.CreateInvitationRequest;
import com.agenciahub.api.entity.User;

public record CreateInvitationCommand(CreateInvitationRequest request, User inviter) {
}
