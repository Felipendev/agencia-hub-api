package com.agenciahub.api.dto.auth;

import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

public record LoginResponse(
        String token,
        UUID userId,
        String name,
        String email,
        UserRole role,
        UUID agencyId,
        String agencyName,
        AgencyStatus agencyStatus,
        SubscriptionStatus subscriptionStatus,
        Instant trialEndsAt,
        Boolean mustChangePassword,
        String publicLinkCode,
        Boolean requiresTermsAcceptance
) {
}
