package com.agenciahub.api.dto.user;

import com.agenciahub.api.domain.UserRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        boolean active,
        BigDecimal commissionPct,
        BigDecimal commissionFixed,
        Instant createdAt,
        boolean termsAccepted
) {
}
