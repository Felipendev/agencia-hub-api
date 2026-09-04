package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.application.persistence.entity.DataDeletionRequest;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.persistence.repository.DataDeletionRequestRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.CustomerStatus;
import com.agenciahub.api.domain.DataDeletionStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Um AGENCY_OWNER só pode processar (e só apaga dado da própria agência) ao atender uma
 * solicitação de exclusão LGPD — nunca de outra agência que compartilhe o mesmo e-mail.
 */
class DataDeletionMultiTenancyIntegrationTest extends AbstractIntegrationTest {
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private PlatformAccountRepository accountRepository;
    @Autowired private CrmCustomerRepository customerRepository;
    @Autowired private DataDeletionRequestRepository requestRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void ownerA_cannotProcessRequestForEmailOnlyPresentInAgencyB() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "titular-" + suffix + "@test.local";

        Agency agencyB = agencyRepository.save(Agency.builder()
                .name("Agency B deletion " + suffix).status(AgencyStatus.ACTIVE)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());
        accountRepository.save(PlatformAccount.builder()
                .agency(agencyB).name("Owner B").email("owner-b-deletion-" + suffix + "@test.local")
                .passwordHash(passwordEncoder.encode("safe-password"))
                .accountKind(AccountKind.AGENCY_OWNER).active(true).emailVerified(true).termsAccepted(true).build());
        CrmCustomer customerB = customerRepository.save(CrmCustomer.builder()
                .agency(agencyB).name("Titular privado B").email(email)
                .status(CustomerStatus.PROSPECT).notes("").build());

        UUID requestId = requestRepository.save(DataDeletionRequest.builder()
                .email(email).status(DataDeletionStatus.PENDING).build()).getId();

        mockMvc.perform(http.authorized(mockMvc, post(IntegrationHttpSupport.API_PREFIX
                        + "/agency/data-deletion-requests/" + requestId + "/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\":\"EXCLUIR\"}")))
                .andExpect(status().isNotFound());

        // Dado de outra agência não foi tocado.
        CrmCustomer stillIntact = customerRepository.findById(customerB.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(email, stillIntact.getEmail());

        // E a solicitação, sem nenhuma agência com dado correspondente tendo processado,
        // continua pendente.
        DataDeletionRequest stillPending = requestRepository.findById(requestId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(DataDeletionStatus.PENDING, stillPending.getStatus());
    }

    @Test
    void ownerA_canProcessRequestForEmailPresentInOwnAgency() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "titular-a-" + suffix + "@test.local";
        String token = http.loginOwner(mockMvc);
        UUID agencyAId = accountRepository.findByEmail(IntegrationHttpSupport.SEED_OWNER_EMAIL)
                .orElseThrow().getAgency().getId();

        customerRepository.save(CrmCustomer.builder()
                .agency(agencyRepository.getReferenceById(agencyAId)).name("Titular próprio A").email(email)
                .status(CustomerStatus.PROSPECT).notes("").build());

        UUID requestId = requestRepository.save(DataDeletionRequest.builder()
                .email(email).status(DataDeletionStatus.PENDING).build()).getId();

        mockMvc.perform(post(IntegrationHttpSupport.API_PREFIX + "/agency/data-deletion-requests/" + requestId + "/process")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\":\"EXCLUIR\"}"))
                .andExpect(status().isOk());

        DataDeletionRequest processed = requestRepository.findById(requestId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(DataDeletionStatus.PROCESSED, processed.getStatus());
    }
}
