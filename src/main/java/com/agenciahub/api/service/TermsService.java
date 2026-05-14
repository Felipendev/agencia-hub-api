package com.agenciahub.api.service;

import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TermsService {

    public static final String CURRENT_TERMS_VERSION = "1.0.0";

    private final UserRepository userRepository;

    public Map<String, String> getLatestTermsMeta() {
        return Map.of(
                "version", CURRENT_TERMS_VERSION,
                "title", "Termos de Uso - AgenciaHub",
                "url", "/termos");
    }

    @Transactional
    public Map<String, String> acceptTerms(UUID userId, String termsVersion) {
        if (!CURRENT_TERMS_VERSION.equals(termsVersion)) {
            throw new IllegalArgumentException("versão dos termos inválida");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + userId));
        if (!Boolean.TRUE.equals(user.getTermsAccepted())) {
            user.setTermsAccepted(true);
            userRepository.save(user);
        }
        return Map.of("message", "termos aceitos com sucesso");
    }
}
