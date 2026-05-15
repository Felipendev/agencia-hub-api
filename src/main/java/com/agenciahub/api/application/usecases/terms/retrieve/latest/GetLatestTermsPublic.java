package com.agenciahub.api.application.usecases.terms.retrieve.latest;

import com.agenciahub.api.application.usecases.terms.shared.TermsConstants;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GetLatestTermsPublic implements GetLatestTermsPublicUseCase {

    @Override
    public Map<String, String> execute(Void unused) {
        return Map.of(
                "version", TermsConstants.CURRENT_TERMS_VERSION,
                "title", "Termos de Uso - AgenciaHub",
                "url", "/termos");
    }
}
