package com.agenciahub.api.application.auth;

import com.agenciahub.api.domain.VerificationCodeType;
import com.agenciahub.api.dto.auth.ForgotPasswordRequest;
import com.agenciahub.api.repository.UserRepository;
import com.agenciahub.api.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ForgotPassword implements ForgotPasswordUseCase {

    private final UserRepository userRepository;
    private final VerificationCodeService verificationCodeService;

    @Override
    public Map<String, String> execute(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();

        userRepository
                .findByEmail(email)
                .ifPresent(user -> verificationCodeService.generateAndSend(
                        email, VerificationCodeType.PASSWORD_RESET, user, user.getName()));

        return Map.of("message", "se o e-mail estiver cadastrado, você receberá um código");
    }
}
