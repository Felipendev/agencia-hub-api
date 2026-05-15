package com.agenciahub.api.application.usecases.auth.forgotpassword;

import com.agenciahub.api.domain.VerificationCodeType;
import com.agenciahub.api.application.usecases.auth.forgotpassword.ForgotPasswordRequestDTO;
import com.agenciahub.api.repository.PlatformAccountRepository;
import com.agenciahub.api.application.integrations.verification.VerificationCodePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ForgotPassword implements ForgotPasswordUseCase {

    private final PlatformAccountRepository userRepository;
    private final VerificationCodePort verificationCodePort;

    @Override
    public Map<String, String> execute(ForgotPasswordRequestDTO request) {
        String email = request.email().trim().toLowerCase();

        userRepository
                .findByEmail(email)
                .ifPresent(user -> verificationCodePort.generateAndSend(
                        email, VerificationCodeType.PASSWORD_RESET, user, user.getName()));

        return Map.of("message", "se o e-mail estiver cadastrado, você receberá um código");
    }
}
