package com.agenciahub.api.application.invitation;

import com.agenciahub.api.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RevokeInvitation implements RevokeInvitationUseCase {

    private final InvitationService invitationService;

    @Override
    public void execute(RevokeInvitationCommand command) {
        invitationService.revoke(command.invitationId(), command.agencyId());
    }
}
