package com.agenciahub.api.application.usecases.invitation.shared;

import com.agenciahub.api.application.usecases.invitation.shared.InvitationSummaryResponseDTO;
import com.agenciahub.api.application.persistence.entity.Invitation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvitationResponseMapper {

    private final InvitationLinkBuilder invitationLinkBuilder;

    public InvitationSummaryResponseDTO toResponse(Invitation invitation) {
        return new InvitationSummaryResponseDTO(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getToken(),
                invitationLinkBuilder.buildInviteUrl(invitation.getToken()),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt());
    }
}
