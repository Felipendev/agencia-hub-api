package com.agenciahub.api.application.usecases.solicitacao.submission.retrieve.list;

import com.agenciahub.api.application.UseCase;
import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoSubmissionSummaryResponseDTO;

import java.util.List;

public interface ListSolicitacaoSubmissionsForAgencyUseCase
        extends UseCase<ListSubmissionsQuery, List<SolicitacaoSubmissionSummaryResponseDTO>> {
}
