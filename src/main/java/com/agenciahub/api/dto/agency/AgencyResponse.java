package com.agenciahub.api.dto.agency;

import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record AgencyResponse(
        UUID id,
        String name,
        String phone,
        String logoUrl,
        String cnpj,
        String address,
        String commercialEmail,
        AgencyStatus status,
        SubscriptionStatus subscriptionStatus,
        Instant trialEndsAt
) {
}
