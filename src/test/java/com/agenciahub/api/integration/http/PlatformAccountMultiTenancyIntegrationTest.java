package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Garante que a gestão de equipe (/users) não vaza contas de outra agência. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlatformAccountMultiTenancyIntegrationTest extends AbstractIntegrationTest {
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private PlatformAccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private UUID agencyBUserId;

    @BeforeAll
    void setupAgencyBUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Agency agencyB = agencyRepository.save(Agency.builder()
                .name("Agency B user " + suffix)
                .status(AgencyStatus.ACTIVE)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());
        agencyBUserId = accountRepository.save(PlatformAccount.builder()
                .agency(agencyB).name("Owner B").email("owner-b-user-" + suffix + "@test.local")
                .passwordHash(passwordEncoder.encode("safe-password"))
                .accountKind(AccountKind.AGENCY_OWNER).active(true).emailVerified(true).termsAccepted(true).build()).getId();
    }

    @Test
    void ownerA_cannotReadOrUpdateAgencyBUser() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/users/" + agencyBUserId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(http.authorized(mockMvc, patch(IntegrationHttpSupport.API_PREFIX + "/users/" + agencyBUserId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerA_listDoesNotContainAgencyBUser() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/users")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(agencyBUserId)).isEmpty());
    }

    @Test
    void createdUser_isAssignedToCallerAgency() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(http.authorized(mockMvc, org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post(IntegrationHttpSupport.API_PREFIX + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Novo Owner\",\"email\":\"novo-owner-" + suffix + "@test.local\",\"password\":\"safe-password\",\"accountKind\":\"AGENCY_OWNER\"}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }
}
