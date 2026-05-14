package com.agenciahub.api.application.solicitacao;

import java.util.UUID;

public record DeleteSolicitacaoSubmissionCommand(UUID submissionId, UUID agencyId) {
}
