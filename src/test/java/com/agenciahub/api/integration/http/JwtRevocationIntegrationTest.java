package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

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

    @Autowired
    private AgencyRepository agencyRepository;

    @AfterEach
    void restoreDeletionPendingAgencies() {
        List<Agency> pending = agencyRepository.findAll().stream()
                .filter(a -> a.getStatus() == AgencyStatus.DELETION_PENDING)
                .toList();
        for (Agency a : pending) {
            AgencyStatus prev = a.getStatusBeforeDeletion() != null
                    ? AgencyStatus.valueOf(a.getStatusBeforeDeletion())
                    : AgencyStatus.TRIAL;
            a.setStatus(prev);
            a.setDeletionScheduledAt(null);
            a.setStatusBeforeDeletion(null);
            agencyRepository.save(a);
        }
    }

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
