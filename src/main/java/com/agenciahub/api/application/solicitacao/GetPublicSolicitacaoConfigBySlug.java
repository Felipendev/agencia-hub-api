package com.agenciahub.api.application.solicitacao;

import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigResponse;
import com.agenciahub.api.service.SolicitacaoConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPublicSolicitacaoConfigBySlug implements GetPublicSolicitacaoConfigBySlugUseCase {

    private final SolicitacaoConfigService solicitacaoConfigService;

    @Override
    public SolicitacaoConfigResponse execute(String slug) {
        return solicitacaoConfigService.getPublicBySlug(slug);
    }
}
