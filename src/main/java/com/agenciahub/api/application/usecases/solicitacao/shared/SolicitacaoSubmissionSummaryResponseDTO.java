package com.agenciahub.api.application.usecases.solicitacao.shared;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record SolicitacaoSubmissionSummaryResponseDTO(
        UUID id,
        String slug,
        Instant createdAt,
        String nome,
        String email,
        String telefone,
        UUID referralSellerId,
        String referralSellerName,
        JsonNode detalhes,
        String observacoes,
        String status,
        Instant statusUpdatedAt,
        Instant convertedAt
) {
    /** Compatibilidade com consumidores que ainda não exibem o ciclo de vida. */
    public SolicitacaoSubmissionSummaryResponseDTO(
            UUID id, String slug, Instant createdAt, String nome, String email, String telefone,
            UUID referralSellerId, String referralSellerName, JsonNode detalhes, String observacoes) {
        this(id, slug, createdAt, nome, email, telefone, referralSellerId, referralSellerName,
                detalhes, observacoes, "PENDING", createdAt, null);
    }
}
