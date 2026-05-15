package com.agenciahub.api.application.auth;

import com.agenciahub.api.application.invitation.InvitationTokenPolicy;
import com.agenciahub.api.dto.auth.InviteValidationResponse;
import com.agenciahub.api.entity.Invitation;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidateInviteToken implements ValidateInviteTokenUseCase {

    private final InvitationRepository invitationRepository;

    @Override
    public InviteValidationResponse execute(String token) {
        Invitation invitation = invitationRepository
                .findWithAgencyByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("convite não encontrado: " + token));
        InvitationTokenPolicy.ensureUsable(invitation);
        return new InviteValidationResponse(invitation.getEmail(), invitation.getAgency().getName());
    }
}
