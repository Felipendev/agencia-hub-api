package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.solicitacao.getpublicsolicitacaoconfigbyslug.GetPublicSolicitacaoConfigBySlugUseCase;
import com.agenciahub.api.application.controller.doc.PublicSolicitacaoConfigAPI;
import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PublicSolicitacaoConfigController implements PublicSolicitacaoConfigAPI {

    private final GetPublicSolicitacaoConfigBySlugUseCase getPublicSolicitacaoConfigBySlugUseCase;

    @Override
    public SolicitacaoConfigResponse getBySlug(String slug) {
        return getPublicSolicitacaoConfigBySlugUseCase.execute(slug);
    }
}
