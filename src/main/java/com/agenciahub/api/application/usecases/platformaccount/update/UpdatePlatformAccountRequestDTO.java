package com.agenciahub.api.application.usecases.platformaccount.update;

import java.math.BigDecimal;

/** Partial update — null fields are ignored. */
public record UpdatePlatformAccountRequestDTO(
        String name,
        String password,
        Boolean active,
        BigDecimal commissionPct,
        BigDecimal commissionFixed
) {
}
