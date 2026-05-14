package com.agenciahub.api.application.terms;

import com.agenciahub.api.service.TermsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GetLatestTermsPublic implements GetLatestTermsPublicUseCase {

    private final TermsService termsService;

    @Override
    public Map<String, String> execute(Void unused) {
        return termsService.getLatestTermsMeta();
    }
}
