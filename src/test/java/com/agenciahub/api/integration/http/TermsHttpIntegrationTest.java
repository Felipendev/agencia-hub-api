package com.agenciahub.api.integration.http;
import static com.agenciahub.api.support.IntegrationHttpSupport.unauthenticated;

import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TermsHttpIntegrationTest extends AbstractIntegrationTest {

    @Test
    void latestPublic_returns200() throws Exception {
        mockMvc.perform(get(IntegrationHttpSupport.API_PREFIX + "/public/terms/latest"))
                .andExpect(status().isOk());
    }

    @Test
    void accept_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post(IntegrationHttpSupport.API_PREFIX + "/terms/accept"))
                .andExpect(unauthenticated());
    }

    @Test
    void accept_withAuth_returns201() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/terms/accept")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"termsVersion\":\"1.0.0\"}")))
                .andExpect(status().isCreated());
    }

    @Test
    void accept_withAuth_wrongVersion_returns400() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/terms/accept")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"termsVersion\":\"0.0.1\"}")))
                .andExpect(status().isBadRequest());
    }
}
