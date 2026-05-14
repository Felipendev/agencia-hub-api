package com.agenciahub.api.application.auth;

import com.agenciahub.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChangePassword implements ChangePasswordUseCase {

    private final AuthService authService;

    @Override
    public Map<String, String> execute(ChangePasswordCommand command) {
        return authService.changePassword(command.request(), command.userId());
    }
}
