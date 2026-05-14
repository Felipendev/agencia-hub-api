package com.agenciahub.api.application.solicitacao;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigResponse;

import java.util.UUID;

public interface GetOrCreateSolicitacaoConfigForAgencyUseCase
        extends UseCase<UUID, SolicitacaoConfigResponse> {
}
