package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.entity.Quotation;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.application.persistence.repository.QuotationRepository;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.CustomerStatus;
import com.agenciahub.api.domain.QuotationStatus;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuotationMultiTenancyIntegrationTest extends AbstractIntegrationTest {
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private PlatformAccountRepository accountRepository;
    @Autowired private CrmCustomerRepository customerRepository;
    @Autowired private QuotationRepository quotationRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private UUID agencyBQuotationId;

    @BeforeAll
    void setupAgencyBQuotation() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Agency agencyB = agencyRepository.save(Agency.builder()
                .name("Agency B quotation " + suffix).status(AgencyStatus.ACTIVE)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());
        PlatformAccount ownerB = accountRepository.save(PlatformAccount.builder()
                .agency(agencyB).name("Owner B").email("owner-b-quote-" + suffix + "@test.local")
                .passwordHash(passwordEncoder.encode("safe-password"))
                .accountKind(AccountKind.AGENCY_OWNER).active(true).emailVerified(true).termsAccepted(true).build());
        CrmCustomer customerB = customerRepository.save(CrmCustomer.builder()
                .agency(agencyB).name("Private B customer " + suffix).email("customer-b-" + suffix + "@test.local")
                .interestDestination("Europa").status(CustomerStatus.PROSPECT).notes("").build());
        agencyBQuotationId = quotationRepository.save(Quotation.builder()
                .agency(agencyB).customer(customerB).createdByUser(ownerB)
                .title("Private B quotation " + suffix).destination("Europa").description("")
                .totalAmount(BigDecimal.TEN).status(QuotationStatus.DRAFT).validUntil(LocalDate.now().plusDays(10))
                .build()).getId();
    }

    @Test
    void ownerA_cannotReadUpdateOrDeleteAgencyBQuotation() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/quotations/" + agencyBQuotationId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(http.authorized(mockMvc, patch(IntegrationHttpSupport.API_PREFIX + "/quotations/" + agencyBQuotationId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"attempt\"}")))
                .andExpect(status().isNotFound());
        mockMvc.perform(http.authorized(mockMvc, delete(IntegrationHttpSupport.API_PREFIX + "/quotations/" + agencyBQuotationId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerA_listDoesNotContainAgencyBQuotation() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/quotations")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(agencyBQuotationId)).isEmpty());
    }
}
