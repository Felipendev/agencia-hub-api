package com.agenciahub.api.application.usecases.solicitacao.config.retrieve;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoConfigSummaryResponseDTO;

import java.util.UUID;

public interface GetOrCreateSolicitacaoConfigForAgencyUseCase
        extends UseCase<UUID, SolicitacaoConfigSummaryResponseDTO> {
}
