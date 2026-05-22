package com.agenciahub.api.integration.http;

import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.Test;

import static com.agenciahub.api.support.IntegrationHttpSupport.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SEC-08: JWT revocation via last_logout_at.
 */
class JwtRevocationIntegrationTest extends AbstractIntegrationTest {

    private static final String AGENCY_URL = IntegrationHttpSupport.API_PREFIX + "/agency";
    private static final String LOGOUT_URL = IntegrationHttpSupport.API_PREFIX + "/auth/logout";
    private static final String DELETE_ACCOUNT_URL = IntegrationHttpSupport.API_PREFIX + "/auth/account";

    @Test
    void logout_withoutAuth_returns401or403() throws Exception {
        mockMvc.perform(post(LOGOUT_URL))
                .andExpect(unauthenticated());
    }

    @Test
    void logout_thenUseToken_returns401() throws Exception {
        String token = http.loginOwner(mockMvc);

        mockMvc.perform(get(AGENCY_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Ensure iat (second precision) < lastLogoutAt
        Thread.sleep(1001);

        mockMvc.perform(post(LOGOUT_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get(AGENCY_URL).header("Authorization", "Bearer " + token))
                .andExpect(unauthenticated());
    }

    @Test
    void requestDeletion_invalidatesAllAgencyTokens() throws Exception {
        String token = http.loginOwner(mockMvc);

        mockMvc.perform(get(AGENCY_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        Thread.sleep(1001);

        mockMvc.perform(delete(DELETE_ACCOUNT_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get(AGENCY_URL).header("Authorization", "Bearer " + token))
                .andExpect(unauthenticated());
    }
}
