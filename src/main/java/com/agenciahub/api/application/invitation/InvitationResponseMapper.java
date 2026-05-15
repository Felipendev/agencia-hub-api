package com.agenciahub.api.application.invitation;

import com.agenciahub.api.dto.invitation.InvitationResponse;
import com.agenciahub.api.entity.Invitation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvitationResponseMapper {

    private final InvitationLinkBuilder invitationLinkBuilder;

    public InvitationResponse toResponse(Invitation invitation) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getToken(),
                invitationLinkBuilder.buildInviteUrl(invitation.getToken()),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt());
    }
}
