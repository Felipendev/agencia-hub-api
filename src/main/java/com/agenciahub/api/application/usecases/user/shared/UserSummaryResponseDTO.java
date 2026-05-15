package com.agenciahub.api.application.usecases.user.shared;

import com.agenciahub.api.domain.enums.AccountKind;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UserSummaryResponseDTO(
        UUID id,
        String name,
        String email,
        AccountKind role,
        boolean active,
        BigDecimal commissionPct,
        BigDecimal commissionFixed,
        Instant createdAt,
        boolean termsAccepted
) {
}
