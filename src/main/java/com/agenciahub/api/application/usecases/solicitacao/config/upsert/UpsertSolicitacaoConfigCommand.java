package com.agenciahub.api.application.usecases.solicitacao.config.upsert;

import com.agenciahub.api.application.usecases.solicitacao.config.upsert.SolicitacaoConfigRequestDTO;

import java.util.UUID;

public record UpsertSolicitacaoConfigCommand(UUID agencyId, SolicitacaoConfigRequestDTO request) {
}
