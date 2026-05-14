package com.agenciahub.api.application.solicitacao;

import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigResponse;
import com.agenciahub.api.service.SolicitacaoConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetOrCreateSolicitacaoConfigForAgency implements GetOrCreateSolicitacaoConfigForAgencyUseCase {

    private final SolicitacaoConfigService solicitacaoConfigService;

    @Override
    public SolicitacaoConfigResponse execute(UUID agencyId) {
        return solicitacaoConfigService.getOrCreateForAgency(agencyId);
    }
}
