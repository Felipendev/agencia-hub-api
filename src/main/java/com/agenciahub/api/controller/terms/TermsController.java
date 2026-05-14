package com.agenciahub.api.controller.terms;

import com.agenciahub.api.application.terms.AcceptTermsCommand;
import com.agenciahub.api.application.terms.AcceptTermsUseCase;
import com.agenciahub.api.application.terms.GetLatestTermsPublicUseCase;
import com.agenciahub.api.controller.terms.docs.TermsAPI;
import com.agenciahub.api.dto.terms.AcceptTermsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TermsController implements TermsAPI {

    private final GetLatestTermsPublicUseCase getLatestTermsPublicUseCase;
    private final AcceptTermsUseCase acceptTermsUseCase;

    @Override
    public Map<String, String> getLatest() {
        return getLatestTermsPublicUseCase.execute(null);
    }

    @Override
    public Map<String, String> accept(AcceptTermsRequest request) {
        UUID userId = getCurrentUserId();
        return acceptTermsUseCase.execute(new AcceptTermsCommand(userId, request.termsVersion()));
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("usuário não autenticado");
        }
        return UUID.fromString(authentication.getName());
    }
}
