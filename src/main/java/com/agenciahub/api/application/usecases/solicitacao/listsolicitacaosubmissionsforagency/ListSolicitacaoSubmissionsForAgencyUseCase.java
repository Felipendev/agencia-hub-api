package com.agenciahub.api.application.usecases.solicitacao.listsolicitacaosubmissionsforagency;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.dto.solicitacao.SolicitacaoSubmissionResponse;

import java.util.List;
import java.util.UUID;

public interface ListSolicitacaoSubmissionsForAgencyUseCase
        extends UseCase<UUID, List<SolicitacaoSubmissionResponse>> {
}
