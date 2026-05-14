package com.agenciahub.api.application.auth;

import com.agenciahub.api.dto.auth.VerifyEmailRequest;
import com.agenciahub.api.dto.auth.VerifyEmailResponse;
import com.agenciahub.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VerifyEmail implements VerifyEmailUseCase {

    private final AuthService authService;

    @Override
    public VerifyEmailResponse execute(VerifyEmailRequest request) {
        return authService.verifyEmail(request);
    }
}
