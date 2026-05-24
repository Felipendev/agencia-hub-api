package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.controller.doc.PublicSolicitacaoConsentRevokeAPI;
import com.agenciahub.api.application.usecases.solicitacao.pub.consent.RevokeConsentRequestDTO;
import com.agenciahub.api.application.usecases.solicitacao.pub.consent.RevokeConsentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PublicSolicitacaoConsentRevokeController implements PublicSolicitacaoConsentRevokeAPI {

    private final RevokeConsentUseCase revokeConsentUseCase;

    @Override
    public void revokeConsent(RevokeConsentRequestDTO body) {
        revokeConsentUseCase.execute(body);
    }
}
