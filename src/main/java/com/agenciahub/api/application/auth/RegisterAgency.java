package com.agenciahub.api.application.auth;

import com.agenciahub.api.dto.auth.RegisterAgencyRequest;
import com.agenciahub.api.dto.auth.RegisterAgencyResponse;
import com.agenciahub.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterAgency implements RegisterAgencyUseCase {

    private final AuthService authService;

    @Override
    public RegisterAgencyResponse execute(RegisterAgencyRequest request) {
        return authService.register(request);
    }
}
