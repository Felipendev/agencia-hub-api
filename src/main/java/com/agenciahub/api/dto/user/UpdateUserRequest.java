package com.agenciahub.api.dto.user;

import java.math.BigDecimal;

/** Partial update — null fields are ignored. */
public record UpdateUserRequest(
        String name,
        String password,
        Boolean active,
        BigDecimal commissionPct,
        BigDecimal commissionFixed
) {
}
