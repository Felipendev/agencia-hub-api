package com.agenciahub.api.application.usecases.solicitacao.submission.retrieve.list;

import com.agenciahub.api.domain.enums.AccountKind;

import java.util.UUID;

public record ListSubmissionsQuery(UUID agencyId, UUID currentUserId, AccountKind accountKind) {
}
