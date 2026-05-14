package com.agenciahub.api.application.auth;

import com.agenciahub.api.dto.auth.InviteValidationResponse;
import com.agenciahub.api.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidateInviteToken implements ValidateInviteTokenUseCase {

    private final InvitationService invitationService;

    @Override
    public InviteValidationResponse execute(String token) {
        return invitationService.validateTokenDetails(token);
    }
}
