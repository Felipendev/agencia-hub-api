package com.agenciahub.api.scheduling;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.repository.AgencyAuditLogRepository;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.persistence.repository.FinancialEntryRepository;
import com.agenciahub.api.application.persistence.repository.InvitationRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.application.persistence.repository.QuotationRepository;
import com.agenciahub.api.application.persistence.repository.SolicitacaoConfigRepository;
import com.agenciahub.api.application.persistence.repository.SolicitacaoSubmissionRepository;
import com.agenciahub.api.application.persistence.repository.VerificationCodeRepository;
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
    private final SolicitacaoSubmissionRepository submissionRepository;
    private final SolicitacaoConfigRepository configRepository;
    private final InvitationRepository invitationRepository;
    private final AgencyAuditLogRepository auditLogRepository;
    private final VerificationCodeRepository verificationCodeRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void executeScheduledDeletions() {
        List<Agency> toDelete = agencyRepository.findByStatusAndDeletionScheduledAtBefore(
                AgencyStatus.DELETION_PENDING, Instant.now());

        for (Agency agency : toDelete) {
            UUID agencyId = agency.getId();
            log.info("Starting hard-delete of agency {} scheduled for {}", agencyId, agency.getDeletionScheduledAt());

            List<UUID> userIds = platformAccountRepository.findByAgency_Id(agencyId)
                    .stream().map(PlatformAccount::getId).toList();

            verificationCodeRepository.deleteByUserIdIn(userIds);
            auditLogRepository.deleteAll(auditLogRepository.findByAgency_IdOrderByCreatedAtDesc(agencyId));
            invitationRepository.deleteAll(invitationRepository.findByAgency_IdOrderByCreatedAtDesc(agencyId));
            submissionRepository.deleteAll(submissionRepository.findByAgency_IdOrderByCreatedAtDesc(agencyId));
            configRepository.deleteAll(configRepository.findAllByAgencyId(agencyId));
            quotationRepository.deleteAll(quotationRepository.findByAgency_Id(agencyId));
            financialEntryRepository.deleteAll(financialEntryRepository.findByAgency_Id(agencyId));
            customerRepository.deleteAll(customerRepository.findByAgency_Id(agencyId));
            platformAccountRepository.deleteAll(platformAccountRepository.findByAgency_Id(agencyId));
            agencyRepository.delete(agency);

            log.info("Hard-deleted agency {}", agencyId);
        }
    }
}
