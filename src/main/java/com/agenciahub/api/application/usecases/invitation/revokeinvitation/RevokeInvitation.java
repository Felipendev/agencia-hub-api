package com.agenciahub.api.application.usecases.invitation.revokeinvitation;

import com.agenciahub.api.domain.InvitationStatus;
import com.agenciahub.api.entity.Invitation;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RevokeInvitation implements RevokeInvitationUseCase {

    private final InvitationRepository invitationRepository;

    @Override
    public void execute(RevokeInvitationCommand command) {
        Invitation invitation = invitationRepository
                .findById(command.invitationId())
                .orElseThrow(() -> new ResourceNotFoundException("convite não encontrado: " + command.invitationId()));

        if (!invitation.getAgency().getId().equals(command.agencyId())) {
            throw new ResourceNotFoundException("convite não encontrado: " + command.invitationId());
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("apenas convites pendentes podem ser revogados");
        }

        invitation.setStatus(InvitationStatus.REVOKED);
        invitationRepository.save(invitation);
    }
}
