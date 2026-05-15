package com.agenciahub.api.application.usecases.solicitacao.submission.delete;

import java.util.UUID;

public record DeleteSolicitacaoSubmissionCommand(UUID submissionId, UUID agencyId) {
}
