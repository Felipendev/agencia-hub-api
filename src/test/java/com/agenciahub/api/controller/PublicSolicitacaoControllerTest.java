package com.agenciahub.api.controller;

import com.agenciahub.api.dto.solicitacao.SolicitacaoConfigResponse;
import com.agenciahub.api.security.JwtAuthFilter;
import com.agenciahub.api.security.RateLimitFilter;
import com.agenciahub.api.service.SolicitacaoConfigService;
import com.agenciahub.api.support.WebMvcControllerTestImports;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PublicSolicitacaoController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcControllerTestImports.class)
class PublicSolicitacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private SolicitacaoConfigService solicitacaoConfigService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getBySlug_returnsConfig() throws Exception {
        var links = objectMapper.createArrayNode();
        when(solicitacaoConfigService.getPublicBySlug("demo"))
                .thenReturn(new SolicitacaoConfigResponse(
                        "demo", "Título", "Intro", null, "Marca", links));

        mockMvc.perform(get("/public/solicitacao-config/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("demo"))
                .andExpect(jsonPath("$.tituloPagina").value("Título"));
    }

    @Test
    void getBySlug_whenServiceThrowsUnhandled_returns500() throws Exception {
        when(solicitacaoConfigService.getPublicBySlug(anyString()))
                .thenThrow(new RuntimeException("simulated failure"));

        mockMvc.perform(get("/public/solicitacao-config/demo"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }
}
