package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.FinancialEntry;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.FinancialEntryRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.FinancialEntryCategory;
import com.agenciahub.api.domain.FinancialEntryStatus;
import com.agenciahub.api.domain.FinancialEntryType;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FinancialEntryMultiTenancyIntegrationTest extends AbstractIntegrationTest {
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private PlatformAccountRepository accountRepository;
    @Autowired private FinancialEntryRepository financialEntryRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private UUID agencyBEntryId;

    @BeforeAll
    void setupAgencyBEntry() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Agency agencyB = agencyRepository.save(Agency.builder()
                .name("Agency B financial " + suffix)
                .status(AgencyStatus.ACTIVE)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());
        accountRepository.save(PlatformAccount.builder()
                .agency(agencyB).name("Owner B").email("owner-b-fin-" + suffix + "@test.local")
                .passwordHash(passwordEncoder.encode("safe-password"))
                .accountKind(AccountKind.AGENCY_OWNER).active(true).emailVerified(true).termsAccepted(true).build());
        agencyBEntryId = financialEntryRepository.save(FinancialEntry.builder()
                .agency(agencyB).description("Private B entry " + suffix)
                .type(FinancialEntryType.INCOME).category(FinancialEntryCategory.PACKAGE_SOLD)
                .amount(BigDecimal.TEN).entryDate(LocalDate.now()).status(FinancialEntryStatus.CONFIRMED).build()).getId();
    }

    @Test
    void ownerA_cannotReadOrEditAgencyBFinancialEntry() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/financial-entries/" + agencyBEntryId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(http.authorized(mockMvc, patch(IntegrationHttpSupport.API_PREFIX + "/financial-entries/" + agencyBEntryId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"attempt\"}")))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerA_listDoesNotContainAgencyBFinancialEntry() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/financial-entries")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(agencyBEntryId)).isEmpty());
    }
}
