package com.agenciahub.api.application.usecases.platformaccount.shared;

import com.agenciahub.api.domain.enums.AccountKind;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlatformAccountSummaryResponseDTO(
        UUID id,
        String name,
        String email,
        AccountKind accountKind,
        boolean active,
        BigDecimal commissionPct,
        BigDecimal commissionFixed,
        Instant createdAt,
        boolean termsAccepted
) {
}
