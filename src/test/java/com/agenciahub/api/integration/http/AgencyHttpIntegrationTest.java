package com.agenciahub.api.integration.http;
import static com.agenciahub.api.support.IntegrationHttpSupport.unauthenticated;

import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgencyHttpIntegrationTest extends AbstractIntegrationTest {

    @Test
    void get_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get(IntegrationHttpSupport.API_PREFIX + "/agency"))
                .andExpect(unauthenticated());
    }

    @Test
    void get_withAuth_returns200() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/agency")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").isNotEmpty());
    }

    @Test
    void patch_withoutAuth_returns401() throws Exception {
        mockMvc.perform(patch(IntegrationHttpSupport.API_PREFIX + "/agency")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nova\"}"))
                .andExpect(unauthenticated());
    }

    @Test
    void patch_withAuth_emptyBody_returns200() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        patch(IntegrationHttpSupport.API_PREFIX + "/agency")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")))
                .andExpect(status().isOk());
    }
}
