package com.agenciahub.api.application.auth;

import com.agenciahub.api.dto.auth.ResendCodeRequest;
import com.agenciahub.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResendCode implements ResendCodeUseCase {

    private final AuthService authService;

    @Override
    public Map<String, String> execute(ResendCodeRequest request) {
        return authService.resendCode(request);
    }
}
