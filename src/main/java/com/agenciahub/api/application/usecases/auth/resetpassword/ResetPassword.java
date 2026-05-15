package com.agenciahub.api.application.usecases.auth.resetpassword;

import com.agenciahub.api.domain.VerificationCodeType;
import com.agenciahub.api.application.usecases.auth.resetpassword.ResetPasswordRequestDTO;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.UserRepository;
import com.agenciahub.api.application.integrations.verification.VerificationCodePort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResetPassword implements ResetPasswordUseCase {

    private final UserRepository userRepository;
    private final VerificationCodePort verificationCodePort;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Map<String, String> execute(ResetPasswordRequestDTO request) {
        String email = request.email().trim().toLowerCase();

        if (request.newPassword().length() < 8) {
            throw new IllegalArgumentException("a senha deve ter no mínimo 8 caracteres");
        }

        if (!request.newPassword().equals(request.newPasswordConfirmation())) {
            throw new IllegalArgumentException("as senhas não coincidem");
        }

        boolean valid =
                verificationCodePort.verify(email, request.code(), VerificationCodeType.PASSWORD_RESET);
        if (!valid) {
            throw new IllegalArgumentException("código inválido ou expirado");
        }

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + email));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        return Map.of("message", "senha redefinida com sucesso");
    }
}
