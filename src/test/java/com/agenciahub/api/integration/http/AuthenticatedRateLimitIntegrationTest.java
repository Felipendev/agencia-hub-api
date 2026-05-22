package com.agenciahub.api.integration.http;

import com.agenciahub.api.config.RateLimitProperties;
import com.agenciahub.api.security.RateLimitFilter;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SEC-06: rate limiting em endpoints autenticados por token JWT.
 */
class AuthenticatedRateLimitIntegrationTest extends AbstractIntegrationTest {

    private static final String AGENCY_URL = IntegrationHttpSupport.API_PREFIX + "/agency";

    @Autowired
    private RateLimitProperties properties;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void enableRateLimit() {
        properties.setEnabled(true);
    }

    @AfterEach
    void restoreRateLimit() {
        properties.setEnabled(false);
        properties.setAuthenticatedGetPerMinute(300);
        rateLimitFilter.initRules();
    }

    @Test
    void authenticatedGet_exceedingLimit_returns429() throws Exception {
        properties.setAuthenticatedGetPerMinute(2);
        rateLimitFilter.initRules();

        String token = http.loginOwner(mockMvc);
        mockMvc.perform(get(AGENCY_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get(AGENCY_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get(AGENCY_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void twoDistinctTokens_eachHaveSeparateBuckets() throws Exception {
        properties.setAuthenticatedGetPerMinute(1);
        rateLimitFilter.initRules();

        String token1 = http.loginOwner(mockMvc);
        // Delay to ensure different iat → different JWT
        Thread.sleep(1001);
        String token2 = http.loginOwner(mockMvc);

        mockMvc.perform(get(AGENCY_URL).header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk());
        mockMvc.perform(get(AGENCY_URL).header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());

        mockMvc.perform(get(AGENCY_URL).header("Authorization", "Bearer " + token1))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(get(AGENCY_URL).header("Authorization", "Bearer " + token2))
                .andExpect(status().isTooManyRequests());
    }
}
