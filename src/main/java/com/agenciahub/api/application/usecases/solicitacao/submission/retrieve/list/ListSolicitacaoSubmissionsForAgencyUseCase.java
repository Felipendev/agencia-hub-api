package com.agenciahub.api.application.usecases.solicitacao.submission.retrieve.list;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoSubmissionSummaryResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ListSolicitacaoSubmissionsForAgencyUseCase
        extends UseCase<UUID, List<SolicitacaoSubmissionSummaryResponseDTO>> {
}
