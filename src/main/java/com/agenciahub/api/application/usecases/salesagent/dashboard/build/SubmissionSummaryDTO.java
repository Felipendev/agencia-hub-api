package com.agenciahub.api.application.usecases.salesagent.dashboard.build;

import java.time.Instant;
import java.util.UUID;

public record SubmissionSummaryDTO(
        UUID id,
        String nome,
        String email,
        String telefone,
        Instant createdAt
) {
}
