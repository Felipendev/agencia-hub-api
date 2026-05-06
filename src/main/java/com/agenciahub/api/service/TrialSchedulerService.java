package com.agenciahub.api.service;

import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.entity.Agency;
import com.agenciahub.api.repository.AgencyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled service that checks for expired trial agencies daily at 2:00 AM
 * and transitions them to SUSPENDED status.
 */
@Service
@RequiredArgsConstructor
public class TrialSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(TrialSchedulerService.class);

    private final AgencyRepository agencyRepository;

    /**
     * Runs daily at 2:00 AM. Queries agencies with TRIAL subscription status
     * and trial_ends_at in the past, then transitions them to SUSPENDED.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void expireTrials() {
        List<Agency> expiredAgencies = agencyRepository
                .findBySubscriptionStatusAndTrialEndsAtBefore(SubscriptionStatus.TRIAL, Instant.now());

        if (expiredAgencies.isEmpty()) {
            log.debug("No expired trial agencies found");
            return;
        }

        log.info("Found {} expired trial agencies to suspend", expiredAgencies.size());

        for (Agency agency : expiredAgencies) {
            agency.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
            agency.setStatus(AgencyStatus.SUSPENDED);
            agencyRepository.save(agency);
            log.info("Agency {} ({}) suspended due to trial expiration", agency.getId(), agency.getName());
        }
    }
}
