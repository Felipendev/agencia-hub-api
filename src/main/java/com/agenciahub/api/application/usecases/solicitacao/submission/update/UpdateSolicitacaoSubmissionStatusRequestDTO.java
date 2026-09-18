package com.agenciahub.api.application.usecases.solicitacao.submission.update;

import com.agenciahub.api.domain.SolicitacaoSubmissionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSolicitacaoSubmissionStatusRequestDTO(@NotNull SolicitacaoSubmissionStatus status) {
}
