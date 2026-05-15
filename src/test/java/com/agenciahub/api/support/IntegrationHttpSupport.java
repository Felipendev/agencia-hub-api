package com.agenciahub.api.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Helpers HTTP para testes de integração (JWT do seed, prefixo {@code /api/v1}).
 */
public class IntegrationHttpSupport {

    /** Caminhos relativos ao {@code server.servlet.context-path} ({@code /api/v1}); o MockMvc aplica o prefixo automaticamente. */
    public static final String API_PREFIX = "";
    public static final String SEED_OWNER_EMAIL = "admin@agenciahub.com";
    public static final String SEED_OWNER_PASSWORD = "admin123";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String loginOwner(MockMvc mockMvc) throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(SEED_OWNER_EMAIL, SEED_OWNER_PASSWORD);
        String response = mockMvc.perform(post(API_PREFIX + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    public MockHttpServletRequestBuilder authorized(MockMvc mockMvc, MockHttpServletRequestBuilder builder)
            throws Exception {
        return builder.header("Authorization", "Bearer " + loginOwner(mockMvc));
    }

    public JsonNode parse(String json) throws Exception {
        return objectMapper.readTree(json);
    }

    /** Spring Security neste projeto pode responder 401 ou 403 sem credenciais. */
    public static ResultMatcher unauthenticated() {
        return result -> {
            int status = result.getResponse().getStatus();
            assertTrue(
                    status == 401 || status == 403,
                    "expected 401 or 403 but was " + status);
        };
    }
}
