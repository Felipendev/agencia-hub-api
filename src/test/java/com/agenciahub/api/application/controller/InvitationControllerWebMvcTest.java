package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.invitation.create.CreateInvitationUseCase;
import com.agenciahub.api.application.usecases.invitation.retrieve.list.ListInvitationsUseCase;
import com.agenciahub.api.application.usecases.invitation.revoke.RevokeInvitationUseCase;
import com.agenciahub.api.web.GlobalExceptionHandler;
import com.agenciahub.api.domain.InvitationStatus;
import com.agenciahub.api.application.usecases.invitation.shared.InvitationSummaryResponseDTO;
import com.agenciahub.api.security.JwtAuthFilter;
import com.agenciahub.api.security.RateLimitFilter;
import com.agenciahub.api.security.TenantContext;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = InvitationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class InvitationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private CreateInvitationUseCase createInvitationUseCase;

    @MockitoBean
    private ListInvitationsUseCase listInvitationsUseCase;

    @MockitoBean
    private RevokeInvitationUseCase revokeInvitationUseCase;

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
    void list_returnsInvitations() throws Exception {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        when(listInvitationsUseCase.execute(agencyId))
                .thenReturn(List.of(new InvitationSummaryResponseDTO(
                        id,
                        "seller@example.com",
                        "token-abc",
                        "http://localhost:3000/convite/token-abc",
                        InvitationStatus.PENDING,
                        now,
                        now)));

        mockMvc.perform(get("/invitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("seller@example.com"));
    }

    @Test
    void revoke_callsUseCase() throws Exception {
        UUID invitationId = UUID.randomUUID();
        mockMvc.perform(delete("/invitations/" + invitationId)).andExpect(status().isNoContent());
        verify(revokeInvitationUseCase).execute(any());
    }

    @Test
    void create_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
