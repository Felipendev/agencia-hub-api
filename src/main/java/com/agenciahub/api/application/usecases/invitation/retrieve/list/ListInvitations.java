package com.agenciahub.api.application.usecases.invitation.retrieve.list;

import com.agenciahub.api.application.usecases.invitation.shared.InvitationResponseMapper;
import com.agenciahub.api.application.usecases.invitation.shared.InvitationSummaryResponseDTO;
import com.agenciahub.api.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListInvitations implements ListInvitationsUseCase {

    private final InvitationRepository invitationRepository;
    private final InvitationResponseMapper invitationResponseMapper;

    @Override
    public List<InvitationSummaryResponseDTO> execute(UUID agencyId) {
        return invitationRepository.findByAgency_IdOrderByCreatedAtDesc(agencyId).stream()
                .map(invitationResponseMapper::toResponse)
                .toList();
    }
}
