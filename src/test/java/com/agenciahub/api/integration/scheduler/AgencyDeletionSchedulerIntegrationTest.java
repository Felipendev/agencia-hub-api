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
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired private com.agenciahub.api.application.persistence.repository.CrmCustomerRepository customers;

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

        UUID person = customers.save(com.agenciahub.api.application.persistence.entity.CrmCustomer.builder()
                .agency(agency).name("Pessoa com contrato").status(com.agenciahub.api.domain.CustomerStatus.ACTIVE).notes("").build()).getId();
        UUID template = UUID.randomUUID(), contract = UUID.randomUUID();
        UUID savingsTrip = UUID.randomUUID(), savings = UUID.randomUUID();
        jdbc.update("INSERT INTO trips(id,agency_id,customer_id,service_type,status) VALUES(?,?,?,'AEREO','COMPLETED')", savingsTrip, agencyId, person);
        jdbc.update("INSERT INTO trip_savings(id,agency_id,trip_id,data,created_by) VALUES(?,?,?,'{}','Teste')", savings, agencyId, savingsTrip);
        jdbc.update("INSERT INTO trip_savings_versions(id,savings_id,agency_id,version,data,author) VALUES(?,?,?,1,'{}','Teste')", UUID.randomUUID(), savings, agencyId);
        jdbc.update("INSERT INTO trip_savings_evidence(id,savings_id,agency_id,filename,content_type,byte_size,content,author) VALUES(?,?,?,'teste.pdf','application/pdf',1,?,'Teste')", UUID.randomUUID(), savings, agencyId, new byte[]{1});
        jdbc.update("INSERT INTO trip_savings_reports(id,agency_id,customer_id,options,content,author) VALUES(?,?,?,'{}','{}','Teste')", UUID.randomUUID(), agencyId, person);
        jdbc.update("INSERT INTO trip_savings_audit(id,agency_id,resource_type,resource_id,trip_id,customer_id,action,detail,author) VALUES(?,?,'SAVINGS',?, ?,?,'SAVINGS_CREATED','{}','Teste')", UUID.randomUUID(), agencyId, savings, savingsTrip, person);
        jdbc.update("INSERT INTO contract_templates(id,agency_id,product,name,created_by) VALUES(?,?,'MILES','Modelo','Teste')", template, agencyId);
        jdbc.update("INSERT INTO contract_template_versions(id,template_id,agency_id,version,body,author) VALUES(?,?,?,1,'Texto','Teste')", UUID.randomUUID(), template, agencyId);
        jdbc.update("INSERT INTO contracts(id,agency_id,product,customer_id,template_id,template_version,title,created_by) VALUES(?,?,'MILES',?,?,1,'Contrato','Teste')", contract, agencyId, person, template);
        jdbc.update("INSERT INTO contract_documents(id,contract_id,agency_id,version,product,data_revision,template_id,template_version,template_body,rendered_content,data_snapshot,content_sha256,author) VALUES(?,?,?,1,'MILES',1,?,1,'Texto','Texto','{}',?,'Teste')", UUID.randomUUID(), contract, agencyId, template, "a".repeat(64));
        jdbc.update("INSERT INTO contract_attachments(id,contract_id,agency_id,kind,filename,content_type,byte_size,content,author) VALUES(?,?,?,'SUPPORTING','teste.pdf','application/pdf',1,?,'Teste')", UUID.randomUUID(), contract, agencyId, new byte[]{1});
        jdbc.update("INSERT INTO contract_audit(id,agency_id,resource_type,resource_id,customer_id,action,detail,author) VALUES(?,?,'CONTRACT',?,?,'CONTRACT_CREATED','{}','Teste')", UUID.randomUUID(), agencyId, contract, person);
        Agency retained = agencyRepository.save(Agency.builder().name("Outra agência preservada").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.TRIAL).build());
        UUID retainedTemplate = UUID.randomUUID();
        jdbc.update("INSERT INTO contract_templates(id,agency_id,product,name,created_by) VALUES(?,?,'TRAVEL','Preservado','Teste')", retainedTemplate, retained.getId());

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
        for (String table : java.util.List.of("contract_audit", "contract_attachments", "contract_documents", "contracts", "contract_templates", "contract_template_versions")) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE agency_id=?", Long.class, agencyId)).isZero();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM contract_templates WHERE id=?", Long.class, retainedTemplate)).isEqualTo(1);
        for (String table : java.util.List.of("trip_savings_audit", "trip_savings_reports", "trip_savings_evidence", "trip_savings_versions", "trip_savings", "trips")) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE agency_id=?", Long.class, agencyId)).isZero();
        }
    }
}
