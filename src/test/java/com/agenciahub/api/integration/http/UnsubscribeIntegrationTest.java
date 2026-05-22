package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.security.UnsubscribeTokenService;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NOTIF-02: stateless JWT-based unsubscribe endpoint.
 */
class UnsubscribeIntegrationTest extends AbstractIntegrationTest {

    private static final String UNSUBSCRIBE_URL = IntegrationHttpSupport.API_PREFIX + "/public/unsubscribe";
    private static final String SEED_EMAIL = IntegrationHttpSupport.SEED_OWNER_EMAIL;

    @Autowired
    private UnsubscribeTokenService tokenService;

    @Autowired
    private PlatformAccountRepository userRepository;

    @AfterEach
    void restorePreferences() {
        userRepository.findByEmail(SEED_EMAIL).ifPresent(u -> {
            u.setNotifEmailSubmissao(true);
            u.setNotifEmailCotacaoAprovada(true);
            userRepository.save(u);
        });
    }

    @Test
    void validToken_disablesPreference() throws Exception {
        String token = tokenService.generate(SEED_EMAIL, "submissao");

        mockMvc.perform(get(UNSUBSCRIBE_URL)
                        .param("t", token)
                        .param("type", "submissao"))
                .andExpect(status().isOk());

        assertThat(userRepository.findByEmail(SEED_EMAIL).get().getNotifEmailSubmissao())
                .isFalse();
    }

    @Test
    void invalidToken_returns200WithErrorAndNoDbChange() throws Exception {
        mockMvc.perform(get(UNSUBSCRIBE_URL)
                        .param("t", "not-a-valid-token")
                        .param("type", "submissao"))
                .andExpect(status().isOk());

        assertThat(userRepository.findByEmail(SEED_EMAIL).get().getNotifEmailSubmissao())
                .isTrue();
    }

    @Test
    void typeMismatch_returns200WithErrorAndNoDbChange() throws Exception {
        String token = tokenService.generate(SEED_EMAIL, "cotacao_aprovada");

        mockMvc.perform(get(UNSUBSCRIBE_URL)
                        .param("t", token)
                        .param("type", "submissao"))
                .andExpect(status().isOk());

        assertThat(userRepository.findByEmail(SEED_EMAIL).get().getNotifEmailSubmissao())
                .isTrue();
    }
}
