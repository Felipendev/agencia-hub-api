package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.CustomerStatus;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerMultiTenancyIntegrationTest extends AbstractIntegrationTest {
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private PlatformAccountRepository accountRepository;
    @Autowired private CrmCustomerRepository customerRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private UUID agencyBCustomerId;

    @BeforeAll
    void setupAgencyBCustomer() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Agency agencyB = agencyRepository.save(Agency.builder()
                .name("Agency B customer " + suffix)
                .status(AgencyStatus.ACTIVE)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());
        accountRepository.save(PlatformAccount.builder()
                .agency(agencyB).name("Owner B").email("owner-b-customer-" + suffix + "@test.local")
                .passwordHash(passwordEncoder.encode("safe-password"))
                .accountKind(AccountKind.AGENCY_OWNER).active(true).emailVerified(true).termsAccepted(true).build());
        agencyBCustomerId = customerRepository.save(CrmCustomer.builder()
                .agency(agencyB).name("Private B customer " + suffix)
                .email("private-b-" + suffix + "@test.local").phone("11999990000")
                .interestDestination("Europa").status(CustomerStatus.PROSPECT).notes("").build()).getId();
    }

    @Test
    void ownerA_cannotReadUpdateOrDeleteAgencyBCustomer() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/customers/" + agencyBCustomerId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(http.authorized(mockMvc, patch(IntegrationHttpSupport.API_PREFIX + "/customers/" + agencyBCustomerId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"attempt\"}")))
                .andExpect(status().isNotFound());
        mockMvc.perform(http.authorized(mockMvc, delete(IntegrationHttpSupport.API_PREFIX + "/customers/" + agencyBCustomerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerA_listDoesNotContainAgencyBCustomer() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/customers")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(agencyBCustomerId)).isEmpty());
    }
}
