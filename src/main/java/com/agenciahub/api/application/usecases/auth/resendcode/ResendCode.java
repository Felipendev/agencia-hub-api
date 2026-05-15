package com.agenciahub.api.application.usecases.auth.resendcode;

import com.agenciahub.api.domain.VerificationCodeType;
import com.agenciahub.api.dto.auth.ResendCodeRequest;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.UserRepository;
import com.agenciahub.api.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResendCode implements ResendCodeUseCase {

    private final UserRepository userRepository;
    private final VerificationCodeService verificationCodeService;

    @Override
    public Map<String, String> execute(ResendCodeRequest request) {
        String email = request.email().trim().toLowerCase();

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + email));

        boolean sent = verificationCodeService.generateAndSend(
                email, VerificationCodeType.EMAIL_VERIFICATION, user, user.getName());

        if (!sent) {
            throw new IllegalStateException("limite de reenvios atingido. tente novamente mais tarde.");
        }

        return Map.of("message", "código reenviado para " + email);
    }
}
