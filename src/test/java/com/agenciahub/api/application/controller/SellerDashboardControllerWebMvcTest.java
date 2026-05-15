package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.salesagent.dashboard.build.BuildSalesAgentDashboardUseCase;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.application.usecases.salesagent.dashboard.build.SalesAgentDashboardResponseDTO;
import com.agenciahub.api.application.usecases.user.shared.UserSummaryResponseDTO;
import com.agenciahub.api.entity.Agency;
import com.agenciahub.api.entity.PlatformAccount;
import com.agenciahub.api.security.JwtAuthFilter;
import com.agenciahub.api.security.RateLimitFilter;
import com.agenciahub.api.application.usecases.user.retrieve.entity.GetUserEntityByIdUseCase;
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
        controllers = SellerDashboardController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SellerDashboardControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private BuildSalesAgentDashboardUseCase buildSellerDashboardUseCase;

    @MockitoBean
    private GetUserEntityByIdUseCase getUserEntityByIdUseCase;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void myDashboard_returnsPayload() throws Exception {
        UUID agencyId = UUID.randomUUID();
        Agency agency = Agency.builder().id(agencyId).name("Ag").build();
        PlatformAccount seller = PlatformAccount.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .name("Vendedor")
                .email("seller@test.com")
                .passwordHash("x")
                .role(AccountKind.SALES_AGENT)
                .active(true)
                .emailVerified(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                seller,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SALES_AGENT"))));

        var sellerResp = new UserSummaryResponseDTO(
                seller.getId(),
                seller.getName(),
                seller.getEmail(),
                AccountKind.SALES_AGENT,
                true,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                true);
        when(buildSellerDashboardUseCase.execute(any(PlatformAccount.class)))
                .thenReturn(new SalesAgentDashboardResponseDTO(
                        sellerResp,
                        2L,
                        1L,
                        0L,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        List.of()));

        mockMvc.perform(get("/seller-dashboard/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuotations").value(2))
                .andExpect(jsonPath("$.seller.email").value("seller@test.com"));
    }

    @Test
    void sellerDashboard_whenOwner_returnsPayload() throws Exception {
        UUID agencyId = UUID.randomUUID();
        Agency agency = Agency.builder().id(agencyId).name("Ag").build();
        PlatformAccount owner = PlatformAccount.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .name("Owner")
                .email("owner@test.com")
                .passwordHash("x")
                .role(AccountKind.AGENCY_OWNER)
                .active(true)
                .emailVerified(true)
                .build();

        UUID sellerId = UUID.randomUUID();
        PlatformAccount seller = PlatformAccount.builder()
                .id(sellerId)
                .agency(agency)
                .name("Outro")
                .email("other@test.com")
                .passwordHash("x")
                .role(AccountKind.SALES_AGENT)
                .active(true)
                .emailVerified(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                owner,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_AGENCY_OWNER"))));

        when(getUserEntityByIdUseCase.execute(sellerId)).thenReturn(seller);

        var sellerResp = new UserSummaryResponseDTO(
                sellerId,
                "Outro",
                "other@test.com",
                AccountKind.SALES_AGENT,
                true,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                true);
        when(buildSellerDashboardUseCase.execute(eq(seller)))
                .thenReturn(new SalesAgentDashboardResponseDTO(
                        sellerResp,
                        0L,
                        0L,
                        0L,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        List.of()));

        mockMvc.perform(get("/seller-dashboard/" + sellerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seller.id").value(sellerId.toString()));
    }
}
