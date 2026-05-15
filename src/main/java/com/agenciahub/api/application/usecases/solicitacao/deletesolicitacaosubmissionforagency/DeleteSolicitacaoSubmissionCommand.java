package com.agenciahub.api.application.usecases.solicitacao.deletesolicitacaosubmissionforagency;

import java.util.UUID;

public record DeleteSolicitacaoSubmissionCommand(UUID submissionId, UUID agencyId) {
}
