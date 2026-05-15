package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.solicitacao.getpublicsolicitacaoconfigbyslug.GetPublicSolicitacaoConfigBySlugUseCase;
import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigResponse;
import com.agenciahub.api.exception.ResourceNotFoundException;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PublicSolicitacaoConfigController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicSolicitacaoConfigControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private GetPublicSolicitacaoConfigBySlugUseCase getPublicSolicitacaoConfigBySlugUseCase;

    @Test
    void getBySlug_returnsConfig() throws Exception {
        var links = objectMapper.createArrayNode();
        when(getPublicSolicitacaoConfigBySlugUseCase.execute(eq("meu-slug")))
                .thenReturn(new SolicitacaoConfigResponse(
                        "meu-slug",
                        "Orçamento",
                        "Preencha",
                        null,
                        "Marca",
                        links));

        mockMvc.perform(get("/public/solicitacao-config/meu-slug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("meu-slug"))
                .andExpect(jsonPath("$.tituloPagina").value("Orçamento"));
    }

    @Test
    void getBySlug_whenMissing_returns404() throws Exception {
        when(getPublicSolicitacaoConfigBySlugUseCase.execute(eq("unknown")))
                .thenThrow(new ResourceNotFoundException("configuração não encontrada para slug: unknown"));

        mockMvc.perform(get("/public/solicitacao-config/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
