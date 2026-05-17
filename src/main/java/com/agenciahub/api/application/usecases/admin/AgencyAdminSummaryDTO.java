package com.agenciahub.api.application.usecases.admin;

import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record AgencyAdminSummaryDTO(
        UUID id,
        String name,
        AgencyStatus status,
        SubscriptionStatus subscriptionStatus,
        Instant trialEndsAt,
        Instant createdAt,
        Instant deletionScheduledAt,
        long ownerCount,
        long sellerCount,
        long customerCount,
        long quotationCount,
        String ownerEmail
) {}
