package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.solicitacao.pub.submit.SubmitPublicSolicitacaoUseCase;
import com.agenciahub.api.application.usecases.solicitacao.pub.submit.PublicSolicitacaoSubmitRequestDTO;
import com.agenciahub.api.application.usecases.solicitacao.pub.submit.PublicSolicitacaoSubmitResponseDTO;
import com.agenciahub.api.security.JwtAuthFilter;
import com.agenciahub.api.security.RateLimitFilter;
import com.agenciahub.api.web.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PublicSolicitacaoSubmitController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicSolicitacaoSubmitControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private SubmitPublicSolicitacaoUseCase submitPublicSolicitacaoUseCase;

    @Test
    void submit_happyPath_returns201() throws Exception {
        UUID submissionId = UUID.randomUUID();
        when(submitPublicSolicitacaoUseCase.execute(any(PublicSolicitacaoSubmitRequestDTO.class)))
                .thenReturn(new PublicSolicitacaoSubmitResponseDTO(true, submissionId));

        var detalhes = objectMapper.createObjectNode();
        detalhes.put("origem", "São Paulo");
        detalhes.put("destino", "Lisboa");

        String json = """
                {
                  "slug": "orcamento-2026",
                  "nome": "Maria",
                  "email": "maria@example.com",
                  "telefone": "11988887777",
                  "detalhes": %s
                }
                """.formatted(detalhes.toString());

        mockMvc.perform(post("/public/solicitacao/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.id").value(submissionId.toString()));
    }

    @Test
    void submit_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/public/solicitacao/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
