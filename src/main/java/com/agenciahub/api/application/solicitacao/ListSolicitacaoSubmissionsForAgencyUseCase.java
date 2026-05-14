package com.agenciahub.api.application.solicitacao;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.solicitacao.SolicitacaoSubmissionResponse;

import java.util.List;
import java.util.UUID;

public interface ListSolicitacaoSubmissionsForAgencyUseCase
        extends UseCase<UUID, List<SolicitacaoSubmissionResponse>> {
}
