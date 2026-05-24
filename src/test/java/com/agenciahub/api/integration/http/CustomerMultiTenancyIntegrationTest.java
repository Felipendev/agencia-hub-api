package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static com.agenciahub.api.support.IntegrationHttpSupport.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica o isolamento multi-tenant em todas as operações de leitura de CrmCustomer.
 * SEC-01: ListCustomers, GetCustomerById e LookupCustomer devem filtrar por agency_id.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerMultiTenancyIntegrationTest extends AbstractIntegrationTest {

    @Autowired private AgencyRepository agencyRepository;
    @Autowired private PlatformAccountRepository platformAccountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String OWNER_B_PASSWORD = "ownerB-multi-tenancy-test-123";
    private static final String PHONE_A1 = "11999991111";
    private static final String PHONE_B1 = "21999992222";

    private String ownerBEmail;
    private String customerA1Email;
    private String customerB1Email;
    private UUID customerB1Id;

    /**
     * Configura duas agências distintas com um cliente cada, criados via API para garantir
     * que o agency_id é atribuído corretamente pelo TenantContext em CreateCustomer.
     */
    @BeforeAll
    void setupTenants() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        ownerBEmail     = "owner-b-"  + suffix + "@mt.test";
        customerA1Email = "cmt-a1-"   + suffix + "@mt.test";
        customerB1Email = "cmt-b1-"   + suffix + "@mt.test";

        Agency agencyB = agencyRepository.save(Agency.builder()
                .name("Agência B - Multi-Tenancy Test")
                .status(AgencyStatus.ACTIVE)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());

        platformAccountRepository.save(PlatformAccount.builder()
                .agency(agencyB)
                .name("Owner B")
                .email(ownerBEmail)
                .passwordHash(passwordEncoder.encode(OWNER_B_PASSWORD))
                .accountKind(AccountKind.AGENCY_OWNER)
                .active(Boolean.TRUE)
                .emailVerified(Boolean.TRUE)
                .termsAccepted(Boolean.TRUE)
                .build());

        // Customer A1 — criado pelo Owner A (agência seed)
        mockMvc.perform(http.authorized(mockMvc,
                        post("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"name":"Customer A1","email":"%s","phone":"%s",
                                         "interestDestination":"Europa","status":"PROSPECT"}
                                        """.formatted(customerA1Email, PHONE_A1))))
                .andExpect(status().isCreated());

        // Customer B1 — criado pelo Owner B (agência B)
        String tokenB = loginAs(ownerBEmail, OWNER_B_PASSWORD);
        String resp = mockMvc.perform(post("/customers")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Customer B1","email":"%s","phone":"%s",
                                 "interestDestination":"Caribe","status":"PROSPECT"}
                                """.formatted(customerB1Email, PHONE_B1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        customerB1Id = UUID.fromString(objectMapper.readTree(resp).get("id").asText());
    }

    // ── LIST ─────────────────────────────────────────────────────────────────

    @Test
    void list_ownerA_seesOwnCustomerAndNotAgencyBCustomer() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get("/customers")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == '%s')]".formatted(customerA1Email)).isNotEmpty())
                .andExpect(jsonPath("$[?(@.email == '%s')]".formatted(customerB1Email)).isEmpty());
    }

    @Test
    void list_ownerB_seesOwnCustomerAndNotAgencyACustomer() throws Exception {
        String tokenB = loginAs(ownerBEmail, OWNER_B_PASSWORD);
        mockMvc.perform(get("/customers").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == '%s')]".formatted(customerB1Email)).isNotEmpty())
                .andExpect(jsonPath("$[?(@.email == '%s')]".formatted(customerA1Email)).isEmpty());
    }

    // ── GET BY ID ────────────────────────────────────────────────────────────

    @Test
    void getById_ownerA_accessingAgencyBCustomer_returns404() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get("/customers/" + customerB1Id)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // ── LOOKUP ───────────────────────────────────────────────────────────────

    @Test
    void lookup_byEmail_ownerA_doesNotSeeAgencyBEmail() throws Exception {
        mockMvc.perform(http.authorized(mockMvc,
                        get("/customers/lookup").param("email", customerB1Email)))
                .andExpect(status().isNotFound());
    }

    @Test
    void lookup_byPhone_ownerA_doesNotSeeAgencyBPhone() throws Exception {
        mockMvc.perform(http.authorized(mockMvc,
                        get("/customers/lookup").param("phone", PHONE_B1)))
                .andExpect(status().isNotFound());
    }

    // ── AUTH ─────────────────────────────────────────────────────────────────

    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(unauthenticated());
    }

    // ── HELPER ───────────────────────────────────────────────────────────────

    private String loginAs(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }
}
