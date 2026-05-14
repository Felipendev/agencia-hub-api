package com.agenciahub.api.application.invitation;

import com.agenciahub.api.dto.invitation.InvitationResponse;
import com.agenciahub.api.entity.Invitation;
import com.agenciahub.api.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvitationResponseMapper {

    private final InvitationService invitationService;

    public InvitationResponse toResponse(Invitation invitation) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getToken(),
                invitationService.buildInviteUrl(invitation.getToken()),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt());
    }
}
