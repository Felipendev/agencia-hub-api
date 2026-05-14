package com.agenciahub.api.application.solicitacao;

import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigRequest;

import java.util.UUID;

public record UpsertSolicitacaoConfigCommand(UUID agencyId, SolicitacaoConfigRequest request) {
}
