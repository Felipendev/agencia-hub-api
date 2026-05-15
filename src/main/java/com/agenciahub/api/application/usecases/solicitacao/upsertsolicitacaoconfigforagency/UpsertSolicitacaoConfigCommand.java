package com.agenciahub.api.application.usecases.solicitacao.upsertsolicitacaoconfigforagency;

import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigRequest;

import java.util.UUID;

public record UpsertSolicitacaoConfigCommand(UUID agencyId, SolicitacaoConfigRequest request) {
}
