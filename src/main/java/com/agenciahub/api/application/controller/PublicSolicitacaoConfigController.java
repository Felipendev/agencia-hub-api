package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.solicitacao.pub.retrieve.byslug.GetPublicSolicitacaoConfigBySlugUseCase;
import com.agenciahub.api.application.controller.doc.PublicSolicitacaoConfigAPI;
import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoConfigSummaryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PublicSolicitacaoConfigController implements PublicSolicitacaoConfigAPI {

    private final GetPublicSolicitacaoConfigBySlugUseCase getPublicSolicitacaoConfigBySlugUseCase;

    @Override
    public SolicitacaoConfigSummaryResponseDTO getBySlug(String slug) {
        return getPublicSolicitacaoConfigBySlugUseCase.execute(slug);
    }
}
