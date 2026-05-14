package com.agenciahub.api.application.auth;

import com.agenciahub.api.dto.auth.RegisterAgencyResponse;
import com.agenciahub.api.dto.auth.RegisterViaInviteRequest;
import com.agenciahub.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterViaInvite implements RegisterViaInviteUseCase {

    private final AuthService authService;

    @Override
    public RegisterAgencyResponse execute(RegisterViaInviteRequest request) {
        return authService.registerViaInvite(request);
    }
}
