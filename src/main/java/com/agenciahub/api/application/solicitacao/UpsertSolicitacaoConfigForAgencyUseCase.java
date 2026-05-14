package com.agenciahub.api.application.solicitacao;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigResponse;

public interface UpsertSolicitacaoConfigForAgencyUseCase
        extends UseCase<UpsertSolicitacaoConfigCommand, SolicitacaoConfigResponse> {
}
