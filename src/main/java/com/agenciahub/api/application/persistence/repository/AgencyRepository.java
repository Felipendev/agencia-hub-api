package com.agenciahub.api.application.persistence.repository;

import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.application.persistence.entity.Agency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AgencyRepository extends JpaRepository<Agency, UUID> {

    List<Agency> findBySubscriptionStatusAndTrialEndsAtBefore(SubscriptionStatus status, Instant now);

    List<Agency> findByStatusAndDeletionScheduledAtBefore(AgencyStatus status, Instant cutoff);
}
