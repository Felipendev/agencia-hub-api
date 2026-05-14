package com.agenciahub.api.application.terms;

import com.agenciahub.api.service.TermsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AcceptTerms implements AcceptTermsUseCase {

    private final TermsService termsService;

    @Override
    public Map<String, String> execute(AcceptTermsCommand command) {
        return termsService.acceptTerms(command.userId(), command.termsVersion(), command.ipAddress());
    }
}
