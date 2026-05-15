package com.agenciahub.api.application.usecases.invitation.create;

import com.agenciahub.api.application.usecases.invitation.create.CreateInvitationRequestDTO;
import com.agenciahub.api.entity.User;

public record CreateInvitationCommand(CreateInvitationRequestDTO request, User inviter) {
}
