package com.agenciahub.api.scheduling;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.persistence.repository.FinancialEntryRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.application.persistence.repository.QuotationRepository;
import com.agenciahub.api.domain.AgencyStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgencyDeletionScheduler {

    private final AgencyRepository agencyRepository;
    private final CrmCustomerRepository customerRepository;
    private final QuotationRepository quotationRepository;
    private final FinancialEntryRepository financialEntryRepository;
    private final PlatformAccountRepository platformAccountRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void executeScheduledDeletions() {
        List<Agency> toDelete = agencyRepository.findByStatusAndDeletionScheduledAtBefore(
                AgencyStatus.DELETION_PENDING, Instant.now());

        for (Agency agency : toDelete) {
            UUID agencyId = agency.getId();
            log.info("Starting hard-delete of agency {} scheduled for {}", agencyId, agency.getDeletionScheduledAt());

            quotationRepository.deleteAll(quotationRepository.findByAgency_Id(agencyId));
            financialEntryRepository.deleteAll(financialEntryRepository.findByAgency_Id(agencyId));
            customerRepository.deleteAll(customerRepository.findByAgency_Id(agencyId));
            platformAccountRepository.deleteAll(platformAccountRepository.findByAgency_Id(agencyId));
            agencyRepository.delete(agency);

            log.info("Hard-deleted agency {}", agencyId);
        }
    }
}
