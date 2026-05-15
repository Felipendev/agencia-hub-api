package com.agenciahub.api.integration.http;

import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthHttpIntegrationTest extends AbstractIntegrationTest {

    @Test
    void login_happyPath_returnsToken() throws Exception {
        http.loginOwner(mockMvc);
    }

    @Test
    void login_wrongPassword_returns404() throws Exception {
        mockMvc.perform(post(IntegrationHttpSupport.API_PREFIX + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@agenciahub.com","password":"wrong-password"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void inviteToken_whenInvalid_returns404() throws Exception {
        mockMvc.perform(get(IntegrationHttpSupport.API_PREFIX + "/auth/invite/invalid-token-xyz"))
                .andExpect(status().isNotFound());
    }

    @Test
    void register_invalidBody_returns400() throws Exception {
        mockMvc.perform(post(IntegrationHttpSupport.API_PREFIX + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
