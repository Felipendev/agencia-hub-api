package com.agenciahub.api.integration.scheduler;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.AgencyAuditLog;
import com.agenciahub.api.application.persistence.entity.Invitation;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.entity.SolicitacaoConfig;
import com.agenciahub.api.application.persistence.entity.VerificationCode;
import com.agenciahub.api.application.persistence.repository.AgencyAuditLogRepository;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.InvitationRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.application.persistence.repository.QuotationRepository;
import com.agenciahub.api.application.persistence.repository.SolicitacaoConfigRepository;
import com.agenciahub.api.application.persistence.repository.SolicitacaoSubmissionRepository;
import com.agenciahub.api.application.persistence.repository.VerificationCodeRepository;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.InvitationStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.domain.VerificationCodeType;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.scheduling.AgencyDeletionScheduler;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DATA-02: agency deletion job clears all related entities without orphan records.
 * Uses a dedicated test agency (created via repositories) so the shared seed is not destroyed.
 */
class AgencyDeletionSchedulerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private AgencyDeletionScheduler scheduler;
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private PlatformAccountRepository userRepository;
    @Autowired private QuotationRepository quotationRepository;
    @Autowired private SolicitacaoConfigRepository configRepository;
    @Autowired private SolicitacaoSubmissionRepository submissionRepository;
    @Autowired private InvitationRepository invitationRepository;
    @Autowired private AgencyAuditLogRepository auditLogRepository;
    @Autowired private VerificationCodeRepository verificationCodeRepository;

    @Test
    void executeScheduledDeletions_clearsAllRelatedEntities() {
        // Create a dedicated test agency (avoids destroying shared seed agency)
        Agency agency = agencyRepository.save(Agency.builder()
                .name("Agência Teste DATA-02")
                .status(AgencyStatus.DELETION_PENDING)
                .statusBeforeDeletion("ACTIVE")
                .subscriptionStatus(SubscriptionStatus.TRIAL)
                .deletionScheduledAt(Instant.now().minusSeconds(10))
                .build());
        UUID agencyId = agency.getId();

        PlatformAccount owner = userRepository.save(PlatformAccount.builder()
                .agency(agency)
                .name("Owner DATA-02")
                .email("data02-owner-" + agencyId + "@test.com")
                .passwordHash("hash")
                .accountKind(AccountKind.AGENCY_OWNER)
                .active(Boolean.TRUE)
                .build());

        // Create one record in each table the scheduler must clean
        verificationCodeRepository.save(VerificationCode.builder()
                .user(owner)
                .email(owner.getEmail())
                .codeHash("hashcode")
                .type(VerificationCodeType.EMAIL_VERIFICATION)
                .used(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build());

        auditLogRepository.save(AgencyAuditLog.builder()
                .agency(agency)
                .user(owner)
                .action("TEST_ACTION")
                .build());

        invitationRepository.save(Invitation.builder()
                .agency(agency)
                .invitedBy(owner)
                .email("invited-" + agencyId + "@test.com")
                .token(UUID.randomUUID().toString())
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(86400))
                .build());

        configRepository.save(SolicitacaoConfig.builder()
                .agency(agency)
                .slug("slug-" + agencyId)
                .tituloPagina("Título Teste")
                .textoIntro("Intro")
                .nomeMarca("Marca Teste")
                .linksSociais(JsonNodeFactory.instance.objectNode())
                .build());

        // Execute the scheduler
        scheduler.executeScheduledDeletions();

        // Verify agency is gone
        assertThat(agencyRepository.findById(agencyId)).isEmpty();

        // Verify no orphan entities remain
        assertThat(userRepository.findByAgency_Id(agencyId)).isEmpty();
        assertThat(quotationRepository.findByAgency_Id(agencyId)).isEmpty();
        assertThat(configRepository.findAllByAgencyId(agencyId)).isEmpty();
        assertThat(submissionRepository.findByAgency_IdOrderByCreatedAtDesc(agencyId)).isEmpty();
        assertThat(invitationRepository.findByAgency_IdOrderByCreatedAtDesc(agencyId)).isEmpty();
        assertThat(auditLogRepository.findByAgency_IdOrderByCreatedAtDesc(agencyId)).isEmpty();
    }
}
