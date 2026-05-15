package com.agenciahub.api.application.usecases.auth.resendcode;

import com.agenciahub.api.domain.VerificationCodeType;
import com.agenciahub.api.application.usecases.auth.resendcode.ResendCodeRequestDTO;
import com.agenciahub.api.entity.PlatformAccount;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.PlatformAccountRepository;
import com.agenciahub.api.application.integrations.verification.VerificationCodePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResendCode implements ResendCodeUseCase {

    private final PlatformAccountRepository userRepository;
    private final VerificationCodePort verificationCodePort;

    @Override
    public Map<String, String> execute(ResendCodeRequestDTO request) {
        String email = request.email().trim().toLowerCase();

        PlatformAccount user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + email));

        boolean sent = verificationCodePort.generateAndSend(
                email, VerificationCodeType.EMAIL_VERIFICATION, user, user.getName());

        if (!sent) {
            throw new IllegalStateException("limite de reenvios atingido. tente novamente mais tarde.");
        }

        return Map.of("message", "código reenviado para " + email);
    }
}
