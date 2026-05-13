package com.agenciahub.api.dto.solicitacao;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record SolicitacaoSubmissionResponse(
        UUID id,
        String slug,
        Instant createdAt,
        String nome,
        String email,
        String telefone,
        UUID referralSellerId,
        String referralSellerName,
        JsonNode detalhes,
        String observacoes
) {
}
