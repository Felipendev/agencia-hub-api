package com.agenciahub.api.integration.http;

import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SEC-04: verifica que o endpoint público de submissão rejeita payloads de detalhes inválidos.
 */
class SolicitacaoDetalhesValidationIntegrationTest extends AbstractIntegrationTest {

    private static final String ENDPOINT = IntegrationHttpSupport.API_PREFIX + "/public/solicitacao/submit";

    private String buildBody(String detalhesJson) {
        return """
                {
                  "slug": "test-sec04",
                  "nome": "Test User",
                  "email": "test@test.com",
                  "telefone": "11999990000",
                  "consentimentoLgpd": true,
                  "detalhes": %s
                }
                """.formatted(detalhesJson);
    }

    @Test
    void submit_withOversizedDetalhes_returns422() throws Exception {
        String bigValue = "x".repeat(70_000);
        String detalhes = """
                {"origem": "%s"}
                """.formatted(bigValue);
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(detalhes)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DETALHES_VALIDATION_ERROR"));
    }

    @Test
    void submit_withUnknownField_returns422() throws Exception {
        String detalhes = """
                {"origem": "São Paulo", "campoMalicioso": "injeção"}
                """;
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(detalhes)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DETALHES_VALIDATION_ERROR"));
    }

    @Test
    void submit_withInvalidDateFormat_returns422() throws Exception {
        String detalhes = """
                {"origem": "São Paulo", "dataIda": "31/12/2025"}
                """;
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(detalhes)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DETALHES_VALIDATION_ERROR"));
    }

    @Test
    void submit_withOutOfRangePassengerCount_returns422() throws Exception {
        String detalhes = """
                {"origem": "São Paulo", "adultos": 99}
                """;
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(detalhes)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DETALHES_VALIDATION_ERROR"));
    }

    @Test
    void submit_withValidDetalhes_returns201() throws Exception {
        String detalhes = """
                {"origem": "São Paulo", "destinosTrechos": ["Paris"], "dataIda": "2025-12-31", "adultos": 2}
                """;
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody(detalhes)))
                .andExpect(status().isCreated());
    }
}
