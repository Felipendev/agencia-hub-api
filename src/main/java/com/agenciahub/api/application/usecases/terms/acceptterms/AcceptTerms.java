package com.agenciahub.api.application.usecases.terms.acceptterms;

import com.agenciahub.api.application.usecases.terms.TermsConstants;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcceptTerms implements AcceptTermsUseCase {

    private final UserRepository userRepository;

    @Override
    public Map<String, String> execute(AcceptTermsCommand command) {
        UUID userId = command.userId();
        String termsVersion = command.termsVersion();
        if (!TermsConstants.CURRENT_TERMS_VERSION.equals(termsVersion)) {
            throw new IllegalArgumentException("versão dos termos inválida");
        }
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + userId));
        if (!Boolean.TRUE.equals(user.getTermsAccepted())) {
            user.setTermsAccepted(true);
            userRepository.save(user);
        }
        return Map.of("message", "termos aceitos com sucesso");
    }
}
