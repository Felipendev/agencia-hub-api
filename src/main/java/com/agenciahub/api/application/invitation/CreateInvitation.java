package com.agenciahub.api.application.invitation;

import com.agenciahub.api.dto.invitation.InvitationResponse;
import com.agenciahub.api.entity.Invitation;
import com.agenciahub.api.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateInvitation implements CreateInvitationUseCase {

    private final InvitationService invitationService;
    private final InvitationResponseMapper invitationResponseMapper;

    @Override
    public InvitationResponse execute(CreateInvitationCommand command) {
        Invitation invitation =
                invitationService.createInvitation(command.request().email(), command.inviter());
        return invitationResponseMapper.toResponse(invitation);
    }
}
