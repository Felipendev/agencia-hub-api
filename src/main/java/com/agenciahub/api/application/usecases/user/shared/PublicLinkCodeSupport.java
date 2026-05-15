package com.agenciahub.api.application.usecases.user.shared;

import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

/** Geração e persistência de {@code public_link_code} para links de vendedor. */
@Component
public class PublicLinkCodeSupport {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public PublicLinkCodeSupport(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String allocate() {
        for (int attempt = 0; attempt < 40; attempt++) {
            StringBuilder sb = new StringBuilder(12);
            for (int i = 0; i < 12; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            String code = sb.toString();
            if (!userRepository.existsByPublicLinkCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("não foi possível gerar um código público único para o usuário");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String ensurePersistedForUserId(UUID userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + userId));
        if (user.getPublicLinkCode() != null && !user.getPublicLinkCode().isBlank()) {
            return user.getPublicLinkCode();
        }
        String code = allocate();
        user.setPublicLinkCode(code);
        userRepository.save(user);
        return code;
    }
}
