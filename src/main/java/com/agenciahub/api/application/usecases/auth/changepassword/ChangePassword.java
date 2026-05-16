package com.agenciahub.api.application.usecases.auth.changepassword;

import com.agenciahub.api.application.usecases.auth.changepassword.ChangePasswordRequestDTO;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChangePassword implements ChangePasswordUseCase {

    private final PlatformAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Map<String, String> execute(ChangePasswordCommand command) {
        ChangePasswordRequestDTO request = command.request();
        UUID currentUserId = command.userId();

        PlatformAccount user = userRepository
                .findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + currentUserId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("senha atual incorreta");
        }

        if (request.newPassword().length() < 8) {
            throw new IllegalArgumentException("a senha deve ter no mínimo 8 caracteres");
        }

        if (!request.newPassword().equals(request.newPasswordConfirmation())) {
            throw new IllegalArgumentException("as senhas não coincidem");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(Instant.now());
        user.setMustChangePassword(Boolean.FALSE);
        userRepository.save(user);

        return Map.of("message", "senha alterada com sucesso");
    }
}
