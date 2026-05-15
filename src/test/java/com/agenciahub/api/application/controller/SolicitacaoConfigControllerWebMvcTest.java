package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.solicitacao.config.retrieve.GetOrCreateSolicitacaoConfigForAgencyUseCase;
import com.agenciahub.api.application.usecases.solicitacao.config.upsert.UpsertSolicitacaoConfigForAgencyUseCase;
import com.agenciahub.api.web.GlobalExceptionHandler;
import com.agenciahub.api.application.usecases.solicitacao.shared.SolicitacaoConfigSummaryResponseDTO;
import com.agenciahub.api.security.JwtAuthFilter;
import com.agenciahub.api.security.RateLimitFilter;
import com.agenciahub.api.security.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SolicitacaoConfigController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SolicitacaoConfigControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private GetOrCreateSolicitacaoConfigForAgencyUseCase getOrCreateSolicitacaoConfigForAgencyUseCase;

    @MockitoBean
    private UpsertSolicitacaoConfigForAgencyUseCase upsertSolicitacaoConfigForAgencyUseCase;

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
    void get_returnsConfig() throws Exception {
        var links = objectMapper.createArrayNode();
        when(getOrCreateSolicitacaoConfigForAgencyUseCase.execute(eq(agencyId)))
                .thenReturn(new SolicitacaoConfigSummaryResponseDTO(
                        "slug", "Título", "Intro", null, "Marca", links));

        mockMvc.perform(get("/agency/solicitacao-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("slug"))
                .andExpect(jsonPath("$.tituloPagina").value("Título"));
    }

    @Test
    void upsert_invalidBody_returns400() throws Exception {
        mockMvc.perform(put("/agency/solicitacao-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
