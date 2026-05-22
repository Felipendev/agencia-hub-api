package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.SolicitacaoSubmission;
import com.agenciahub.api.application.persistence.repository.SolicitacaoSubmissionRepository;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SEC-03: verifica rastreabilidade de consentimento LGPD (auditoria e revogação).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConsentimentoAuditIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SolicitacaoSubmissionRepository submissionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SUBMIT_URL = IntegrationHttpSupport.API_PREFIX + "/public/solicitacao/submit";
    private static final String REVOKE_URL  = IntegrationHttpSupport.API_PREFIX + "/public/solicitacao/consent/revoke";

    private UUID submitConsent(String email, String phone) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String body = """
                {
                  "slug": "sec03-test-%s",
                  "nome": "Test User",
                  "email": "%s",
                  "telefone": "%s",
                  "consentimentoLgpd": true,
                  "detalhes": {"origem": "São Paulo", "destinosTrechos": ["Paris"]}
                }
                """.formatted(suffix, email, phone);
        String resp = mockMvc.perform(post(SUBMIT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(resp).get("id").asText());
    }

    // ── Auditoria de campos ─────────────────────────────────────────────────────

    @Test
    void submit_withConsent_populatesConsentimentoAt() throws Exception {
        UUID id = submitConsent("audit-at@sec03.test", "11988880001");
        SolicitacaoSubmission sub = submissionRepository.findById(id).orElseThrow();
        assertThat(sub.getConsentimentoAt()).isNotNull();
    }

    @Test
    void submit_withConsent_populatesConsentimentoVersaoTermos() throws Exception {
        UUID id = submitConsent("audit-versao@sec03.test", "11988880002");
        SolicitacaoSubmission sub = submissionRepository.findById(id).orElseThrow();
        assertThat(sub.getConsentimentoVersaoTermos()).isNotBlank();
    }

    @Test
    void submit_withConsent_populatesConsentimentoIp() throws Exception {
        UUID id = submitConsent("audit-ip@sec03.test", "11988880003");
        SolicitacaoSubmission sub = submissionRepository.findById(id).orElseThrow();
        assertThat(sub.getConsentimentoIp()).isNotBlank();
    }

    // ── Revogação ──────────────────────────────────────────────────────────────

    @Test
    void revokeConsent_withValidEmailAndPhone_setsConsentimentoLgpdFalse() throws Exception {
        String email = "revoke-valid@sec03.test";
        String phone = "11988880010";
        UUID id = submitConsent(email, phone);

        mockMvc.perform(post(REVOKE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "telefone": "%s"}
                                """.formatted(email, phone)))
                .andExpect(status().isOk());

        SolicitacaoSubmission sub = submissionRepository.findById(id).orElseThrow();
        assertThat(sub.isConsentimentoLgpd()).isFalse();
    }

    @Test
    void revokeConsent_withUnknownEmailAndPhone_returns422() throws Exception {
        mockMvc.perform(post(REVOKE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "naoexiste@sec03.test", "telefone": "11900000099"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void revokeConsent_withEmptyBody_returns400() throws Exception {
        mockMvc.perform(post(REVOKE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── Campos de auditoria não são sobrescritos pelo payload do cliente ───────

    @Test
    void submit_consentimentoVersaoTermos_comesFromConfigNotPayload() throws Exception {
        UUID id = submitConsent("audit-versao2@sec03.test", "11988880004");
        SolicitacaoSubmission sub = submissionRepository.findById(id).orElseThrow();
        // Should be the configured version, not anything the client could inject
        assertThat(sub.getConsentimentoVersaoTermos()).isNotNull();
        assertThat(sub.getConsentimentoVersaoTermos().length()).isLessThanOrEqualTo(32);
    }
}
