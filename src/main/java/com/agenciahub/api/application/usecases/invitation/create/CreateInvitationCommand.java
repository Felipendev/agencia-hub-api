package com.agenciahub.api.application.usecases.invitation.create;

import com.agenciahub.api.application.usecases.invitation.create.CreateInvitationRequestDTO;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;

public record CreateInvitationCommand(CreateInvitationRequestDTO request, PlatformAccount inviter) {
}
