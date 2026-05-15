package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.solicitacao.deletesolicitacaosubmissionforagency.DeleteSolicitacaoSubmissionForAgencyUseCase;
import com.agenciahub.api.application.usecases.solicitacao.listsolicitacaosubmissionsforagency.ListSolicitacaoSubmissionsForAgencyUseCase;
import com.agenciahub.api.web.GlobalExceptionHandler;
import com.agenciahub.api.dto.solicitacao.SolicitacaoSubmissionResponse;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SolicitacaoSubmissionAgencyController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SolicitacaoSubmissionAgencyControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private ListSolicitacaoSubmissionsForAgencyUseCase listSolicitacaoSubmissionsForAgencyUseCase;

    @MockitoBean
    private DeleteSolicitacaoSubmissionForAgencyUseCase deleteSolicitacaoSubmissionForAgencyUseCase;

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
    void list_returnsSubmissions() throws Exception {
        UUID id = UUID.randomUUID();
        Instant created = Instant.parse("2026-01-01T12:00:00Z");
        when(listSolicitacaoSubmissionsForAgencyUseCase.execute(eq(agencyId)))
                .thenReturn(List.of(new SolicitacaoSubmissionResponse(
                        id,
                        "demo",
                        created,
                        "João",
                        "j@t.com",
                        "11999999999",
                        null,
                        null,
                        null,
                        "")));

        mockMvc.perform(get("/agency/solicitacao-submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("João"));
    }

    @Test
    void delete_callsUseCase() throws Exception {
        UUID submissionId = UUID.randomUUID();
        mockMvc.perform(delete("/agency/solicitacao-submissions/" + submissionId))
                .andExpect(status().isNoContent());
        verify(deleteSolicitacaoSubmissionForAgencyUseCase).execute(any());
    }
}
