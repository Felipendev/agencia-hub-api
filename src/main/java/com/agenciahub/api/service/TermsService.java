package com.agenciahub.api.service;

import com.agenciahub.api.entity.TermsAcceptance;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.TermsAcceptanceRepository;
import com.agenciahub.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TermsService {

    private static final String CURRENT_TERMS_VERSION = "1.0.0";

    private final TermsAcceptanceRepository termsAcceptanceRepository;
    private final UserRepository userRepository;

    public Map<String, String> getLatestTermsMeta() {
        return Map.of(
                "version", CURRENT_TERMS_VERSION,
                "title", "Termos de Uso - AgenciaHub",
                "url", "/termos");
    }

    @Transactional
    public Map<String, String> acceptTerms(UUID userId, String termsVersion, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado"));

        TermsAcceptance acceptance = TermsAcceptance.builder()
                .user(user)
                .termsVersion(termsVersion)
                .ipAddress(ipAddress)
                .build();
        termsAcceptanceRepository.save(acceptance);

        return Map.of("message", "termos aceitos com sucesso");
    }
}
