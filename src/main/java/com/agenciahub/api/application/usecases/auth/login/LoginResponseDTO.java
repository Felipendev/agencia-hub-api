package com.agenciahub.api.application.usecases.auth.login;

import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.domain.enums.AccountKind;

import java.time.Instant;
import java.util.UUID;

public record LoginResponseDTO(
        String token,
        UUID userId,
        String name,
        String email,
        AccountKind accountKind,
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
