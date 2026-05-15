package com.agenciahub.api.integration.http;
import static com.agenciahub.api.support.IntegrationHttpSupport.unauthenticated;

import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InvitationHttpIntegrationTest extends AbstractIntegrationTest {

    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get(IntegrationHttpSupport.API_PREFIX + "/invitations"))
                .andExpect(unauthenticated());
    }

    @Test
    void list_withAuth_returns200() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/invitations")))
                .andExpect(status().isOk());
    }

    @Test
    void create_invalidBody_returns400() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/invitations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void revoke_whenMissing_returns404() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        delete(IntegrationHttpSupport.API_PREFIX + "/invitations/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }
}
