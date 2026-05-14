package com.agenciahub.api.application.invitation;

import com.agenciahub.api.dto.invitation.InvitationResponse;
import com.agenciahub.api.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListInvitations implements ListInvitationsUseCase {

    private final InvitationService invitationService;
    private final InvitationResponseMapper invitationResponseMapper;

    @Override
    public List<InvitationResponse> execute(UUID agencyId) {
        return invitationService.listByAgency(agencyId).stream()
                .map(invitationResponseMapper::toResponse)
                .toList();
    }
}
