package com.agenciahub.api.application.scheduling;

import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Job diário: agências em trial expirado passam a SUSPENDED.
 */
@Component
@RequiredArgsConstructor
public class ExpireTrialsScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpireTrialsScheduler.class);

    private final AgencyRepository agencyRepository;

    @Scheduled(cron = "0 0 2 * * *")
    public void expireTrials() {
        List<Agency> expiredAgencies = agencyRepository.findBySubscriptionStatusAndTrialEndsAtBefore(
                SubscriptionStatus.TRIAL, Instant.now());

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
