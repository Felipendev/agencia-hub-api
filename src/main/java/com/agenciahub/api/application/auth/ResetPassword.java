package com.agenciahub.api.application.auth;

import com.agenciahub.api.dto.auth.ResetPasswordRequest;
import com.agenciahub.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResetPassword implements ResetPasswordUseCase {

    private final AuthService authService;

    @Override
    public Map<String, String> execute(ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }
}
