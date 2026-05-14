package com.agenciahub.api.application.solicitacao;

import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigResponse;
import com.agenciahub.api.service.SolicitacaoConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpsertSolicitacaoConfigForAgency implements UpsertSolicitacaoConfigForAgencyUseCase {

    private final SolicitacaoConfigService solicitacaoConfigService;

    @Override
    public SolicitacaoConfigResponse execute(UpsertSolicitacaoConfigCommand command) {
        return solicitacaoConfigService.upsertForAgency(command.agencyId(), command.request());
    }
}
