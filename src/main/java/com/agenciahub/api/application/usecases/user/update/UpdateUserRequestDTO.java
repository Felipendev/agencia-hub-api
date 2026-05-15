package com.agenciahub.api.application.usecases.user.update;

import java.math.BigDecimal;

/** Partial update — null fields are ignored. */
public record UpdateUserRequestDTO(
        String name,
        String password,
        Boolean active,
        BigDecimal commissionPct,
        BigDecimal commissionFixed
) {
}
