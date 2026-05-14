package com.agenciahub.api.application.auth;

import com.agenciahub.api.dto.auth.ForgotPasswordRequest;
import com.agenciahub.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ForgotPassword implements ForgotPasswordUseCase {

    private final AuthService authService;

    @Override
    public Map<String, String> execute(ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }
}
