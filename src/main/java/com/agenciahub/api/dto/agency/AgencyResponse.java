package com.agenciahub.api.dto.agency;

import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record AgencyResponse(
        UUID id,
        String name,
        String phone,
        String logoUrl,
        String cnpj,
        String address,
        JsonNode addressDetails,
        String commercialEmail,
        AgencyStatus status,
        SubscriptionStatus subscriptionStatus,
        Instant trialEndsAt
) {
}
