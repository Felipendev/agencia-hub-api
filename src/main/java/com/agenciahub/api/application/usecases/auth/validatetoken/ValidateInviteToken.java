package com.agenciahub.api.application.usecases.auth.validatetoken;

import com.agenciahub.api.application.usecases.invitation.shared.InvitationTokenPolicy;
import com.agenciahub.api.application.usecases.auth.validatetoken.InviteValidationResponseDTO;
import com.agenciahub.api.application.persistence.entity.Invitation;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidateInviteToken implements ValidateInviteTokenUseCase {

    private final InvitationRepository invitationRepository;

    @Override
    public InviteValidationResponseDTO execute(String token) {
        Invitation invitation = invitationRepository
                .findWithAgencyByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("convite não encontrado: " + token));
        InvitationTokenPolicy.ensureUsable(invitation);
        return new InviteValidationResponseDTO(invitation.getEmail(), invitation.getAgency().getName());
    }
}
