package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.salesagent.dashboard.build.BuildSalesAgentDashboardUseCase;
import com.agenciahub.api.application.usecases.salesagent.dashboard.build.SalesAgentDashboardResponseDTO;
import com.agenciahub.api.application.usecases.platformaccount.retrieve.entity.GetPlatformAccountEntityByIdUseCase;
import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountSummaryResponseDTO;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.security.JwtAuthFilter;
import com.agenciahub.api.security.RateLimitFilter;
import com.agenciahub.api.web.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SalesAgentDashboardController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SalesAgentDashboardControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private BuildSalesAgentDashboardUseCase buildSalesAgentDashboardUseCase;

    @MockitoBean
    private GetPlatformAccountEntityByIdUseCase getUserEntityByIdUseCase;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void myDashboard_returnsPayload() throws Exception {
        UUID agencyId = UUID.randomUUID();
        Agency agency = Agency.builder().id(agencyId).name("Ag").build();
        PlatformAccount agent = PlatformAccount.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .name("Agente")
                .email("agent@test.com")
                .passwordHash("x")
                .accountKind(AccountKind.SALES_AGENT)
                .active(true)
                .emailVerified(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                agent,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SALES_AGENT"))));

        var agentSummary = new PlatformAccountSummaryResponseDTO(
                agent.getId(),
                agent.getName(),
                agent.getEmail(),
                AccountKind.SALES_AGENT,
                true,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                true);
        when(buildSalesAgentDashboardUseCase.execute(any(PlatformAccount.class)))
                .thenReturn(new SalesAgentDashboardResponseDTO(
                        agentSummary,
                        2L,
                        1L,
                        0L,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        List.of()));

        mockMvc.perform(get("/sales-agent/dashboard/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuotations").value(2))
                .andExpect(jsonPath("$.salesAgent.email").value("agent@test.com"));
    }

    @Test
    void agentDashboard_whenOwner_returnsPayload() throws Exception {
        UUID agencyId = UUID.randomUUID();
        Agency agency = Agency.builder().id(agencyId).name("Ag").build();
        PlatformAccount owner = PlatformAccount.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .name("Owner")
                .email("owner@test.com")
                .passwordHash("x")
                .accountKind(AccountKind.AGENCY_OWNER)
                .active(true)
                .emailVerified(true)
                .build();

        UUID agentId = UUID.randomUUID();
        PlatformAccount agent = PlatformAccount.builder()
                .id(agentId)
                .agency(agency)
                .name("Outro")
                .email("other@test.com")
                .passwordHash("x")
                .accountKind(AccountKind.SALES_AGENT)
                .active(true)
                .emailVerified(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_AGENCY_OWNER"))));

        when(getUserEntityByIdUseCase.execute(agentId)).thenReturn(agent);

        var agentSummary = new PlatformAccountSummaryResponseDTO(
                agentId,
                "Outro",
                "other@test.com",
                AccountKind.SALES_AGENT,
                true,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                true);
        when(buildSalesAgentDashboardUseCase.execute(eq(agent)))
                .thenReturn(new SalesAgentDashboardResponseDTO(
                        agentSummary,
                        0L,
                        0L,
                        0L,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        List.of()));

        mockMvc.perform(get("/sales-agent/dashboard/" + agentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesAgent.id").value(agentId.toString()));
    }
}
