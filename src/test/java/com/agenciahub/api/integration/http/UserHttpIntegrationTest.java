package com.agenciahub.api.integration.http;
import static com.agenciahub.api.support.IntegrationHttpSupport.unauthenticated;

import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserHttpIntegrationTest extends AbstractIntegrationTest {

    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get(IntegrationHttpSupport.API_PREFIX + "/users"))
                .andExpect(unauthenticated());
    }

    @Test
    void list_withAuth_returns200() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/users")))
                .andExpect(status().isOk());
    }

    @Test
    void listSalesAgents_withAuth_returns200() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/users/sales-agents")))
                .andExpect(status().isOk());
    }

    @Test
    void get_whenMissing_returns404() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/users/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_salesAgentRole_returns400() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Vendedor",
                                          "email": "v-%s@test.com",
                                          "password": "secret12",
                                          "accountKind": "SALES_AGENT"
                                        }
                                        """.formatted(UUID.randomUUID()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patch_whenMissing_returns404() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        patch(IntegrationHttpSupport.API_PREFIX + "/users/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"X\"}")))
                .andExpect(status().isNotFound());
    }
}
