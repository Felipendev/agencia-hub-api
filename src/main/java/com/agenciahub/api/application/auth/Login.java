package com.agenciahub.api.application.auth;

import com.agenciahub.api.dto.auth.LoginRequest;
import com.agenciahub.api.dto.auth.LoginResponse;
import com.agenciahub.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Login implements LoginUseCase {

    private final AuthService authService;

    @Override
    public LoginResponse execute(LoginRequest request) {
        return authService.login(request);
    }
}
