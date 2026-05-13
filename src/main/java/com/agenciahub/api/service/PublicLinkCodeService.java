package com.agenciahub.api.service;

import com.agenciahub.api.entity.User;
import com.agenciahub.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicLinkCodeService {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    /** Código curto para links públicos (parâmetro ?vendedor=), distinto do UUID do usuário. */
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
        throw new IllegalStateException("Não foi possível gerar código público único para o usuário.");
    }

    /**
     * Garante que o usuário tenha {@code public_link_code} persistido (única fonte de geração).
     * {@code REQUIRES_NEW} permite gravar mesmo quando o chamador está em transação somente leitura (ex.: login).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String ensurePersistedForUserId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado: " + userId));
        if (user.getPublicLinkCode() != null && !user.getPublicLinkCode().isBlank()) {
            return user.getPublicLinkCode();
        }
        String code = allocate();
        user.setPublicLinkCode(code);
        userRepository.save(user);
        return code;
    }
}