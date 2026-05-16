package com.agenciahub.api.application.usecases.solicitacao.config.upsert;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoConfigSummaryResponseDTO;

public interface UpsertSolicitacaoConfigForAgencyUseCase
        extends UseCase<UpsertSolicitacaoConfigCommand, SolicitacaoConfigSummaryResponseDTO> {
}
