package com.agenciahub.api.integration.http;
import static com.agenciahub.api.support.IntegrationHttpSupport.unauthenticated;

import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SalesAgentDashboardHttpIntegrationTest extends AbstractIntegrationTest {

    @Test
    void myDashboard_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get(IntegrationHttpSupport.API_PREFIX + "/sales-agent/dashboard/me"))
                .andExpect(unauthenticated());
    }

    @Test
    void myDashboard_withAuth_returns200() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/sales-agent/dashboard/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesAgent").exists());
    }

    @Test
    void agentDashboard_whenMissing_returns404() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        get(IntegrationHttpSupport.API_PREFIX + "/sales-agent/dashboard/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void agentDashboard_withAuth_existingOwner_returns200() throws Exception {
        String token = http.loginOwner(mockMvc);
        String loginJson = mockMvc.perform(get(IntegrationHttpSupport.API_PREFIX + "/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String ownerId = http.parse(loginJson).get(0).get("id").asText();
        mockMvc.perform(get(IntegrationHttpSupport.API_PREFIX + "/sales-agent/dashboard/" + ownerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
