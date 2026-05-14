package com.agenciahub.api.controller.agency;

import com.agenciahub.api.application.agency.GetAgencyUseCase;
import com.agenciahub.api.application.agency.UpdateAgencyUseCase;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.dto.agency.AgencyResponse;
import com.agenciahub.api.security.JwtAuthFilter;
import com.agenciahub.api.security.RateLimitFilter;
import com.agenciahub.api.security.TenantContext;
import com.agenciahub.api.web.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AgencyController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AgencyControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private GetAgencyUseCase getAgencyUseCase;

    @MockitoBean
    private UpdateAgencyUseCase updateAgencyUseCase;

    private final UUID agencyId = UUID.randomUUID();

    @BeforeEach
    void setTenant() {
        TenantContext.set(agencyId);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void get_returnsAgency() throws Exception {
        when(getAgencyUseCase.execute(eq(agencyId)))
                .thenReturn(new AgencyResponse(
                        agencyId,
                        "Minha agência",
                        "11999999999",
                        null,
                        null,
                        null,
                        null,
                        null,
                        AgencyStatus.TRIAL,
                        SubscriptionStatus.TRIAL,
                        Instant.parse("2026-06-01T00:00:00Z")));

        mockMvc.perform(get("/agency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Minha agência"));
    }

    @Test
    void update_nameExceedsMaxLength_returns400() throws Exception {
        String tooLong = "x".repeat(256);
        mockMvc.perform(patch("/agency")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
