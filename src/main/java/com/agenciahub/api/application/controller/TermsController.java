package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.terms.acceptterms.AcceptTermsCommand;
import com.agenciahub.api.application.usecases.terms.acceptterms.AcceptTermsUseCase;
import com.agenciahub.api.application.usecases.terms.getlatesttermspublic.GetLatestTermsPublicUseCase;
import com.agenciahub.api.application.controller.doc.TermsAPI;
import com.agenciahub.api.dto.terms.AcceptTermsRequest;
import com.agenciahub.api.security.SecurityContextUsers;
import lombok.RequiredArgsConstructor;
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
        UUID userId = SecurityContextUsers.requireUserId();
        return acceptTermsUseCase.execute(new AcceptTermsCommand(userId, request.termsVersion()));
    }
}
