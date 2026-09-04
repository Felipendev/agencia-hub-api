package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.persistence.repository.DataDeletionRequestRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.domain.CustomerStatus;
import com.agenciahub.api.domain.DataDeletionStatus;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static com.agenciahub.api.support.IntegrationHttpSupport.unauthenticated;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LGPD-02: solicitação pública de exclusão de dados e processamento pelo owner.
 */
class DataDeletionIntegrationTest extends AbstractIntegrationTest {

    private static final String PUBLIC_URL = IntegrationHttpSupport.API_PREFIX + "/public/data-deletion-request";
    private static final String PROCESS_URL_TEMPLATE = IntegrationHttpSupport.API_PREFIX
            + "/agency/data-deletion-requests/%s/process";

    @Autowired
    private DataDeletionRequestRepository repository;

    @Autowired
    private CrmCustomerRepository customerRepository;

    @Autowired
    private PlatformAccountRepository accountRepository;

    @Autowired
    private AgencyRepository agencyRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Processar uma solicitação só é permitido para uma agência que tenha dado correspondente
     * ao e-mail (ver TODO-011): cria um cliente da agência do owner de teste com esse e-mail.
     */
    private void giveOwnerAgencyDataFor(String email) {
        UUID ownerAgencyId = accountRepository.findByEmail(IntegrationHttpSupport.SEED_OWNER_EMAIL)
                .orElseThrow().getAgency().getId();
        customerRepository.save(CrmCustomer.builder()
                .agency(agencyRepository.getReferenceById(ownerAgencyId))
                .name("Titular " + email)
                .email(email)
                .status(CustomerStatus.PROSPECT)
                .notes("")
                .build());
    }

    @Test
    void create_withValidEmail_returns201AndCreatesRequest() throws Exception {
        String email = "titular-" + UUID.randomUUID() + "@lgpd02.test";
        String resp = mockMvc.perform(post(PUBLIC_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "motivo": "quero exclusão"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        UUID requestId = UUID.fromString(objectMapper.readTree(resp).get("requestId").asText());
        assertThat(repository.findById(requestId)).isPresent();
        assertThat(repository.findById(requestId).get().getStatus()).isEqualTo(DataDeletionStatus.PENDING);
    }

    @Test
    void create_sameEmailSameDay_returns200Idempotent() throws Exception {
        String email = "idempotente-" + UUID.randomUUID() + "@lgpd02.test";
        String body = """
                {"email": "%s"}
                """.formatted(email);

        mockMvc.perform(post(PUBLIC_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post(PUBLIC_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void create_withInvalidEmail_returns400() throws Exception {
        mockMvc.perform(post(PUBLIC_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nao-e-email"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withMissingEmail_returns400() throws Exception {
        mockMvc.perform(post(PUBLIC_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void process_excluir_byOwner_changesStatusToProcessed() throws Exception {
        String email = "process-" + UUID.randomUUID() + "@lgpd02.test";
        String createResp = mockMvc.perform(post(PUBLIC_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID requestId = UUID.fromString(objectMapper.readTree(createResp).get("requestId").asText());
        giveOwnerAgencyDataFor(email);

        mockMvc.perform(http.authorized(mockMvc,
                        post(PROCESS_URL_TEMPLATE.formatted(requestId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"acao": "EXCLUIR"}
                                        """)))
                .andExpect(status().isOk());

        assertThat(repository.findById(requestId).get().getStatus()).isEqualTo(DataDeletionStatus.PROCESSED);
    }

    @Test
    void process_rejeitar_byOwner_changesStatusToRejected() throws Exception {
        String email = "reject-" + UUID.randomUUID() + "@lgpd02.test";
        String createResp = mockMvc.perform(post(PUBLIC_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID requestId = UUID.fromString(objectMapper.readTree(createResp).get("requestId").asText());
        giveOwnerAgencyDataFor(email);

        mockMvc.perform(http.authorized(mockMvc,
                        post(PROCESS_URL_TEMPLATE.formatted(requestId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"acao": "REJEITAR", "justificativa": "dados necessários legalmente"}
                                        """)))
                .andExpect(status().isOk());

        assertThat(repository.findById(requestId).get().getStatus()).isEqualTo(DataDeletionStatus.REJECTED);
    }

    @Test
    void process_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post(PROCESS_URL_TEMPLATE.formatted(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"acao": "EXCLUIR"}
                                """))
                .andExpect(unauthenticated());
    }

    @Test
    void process_nonExistentRequest_returns404() throws Exception {
        mockMvc.perform(http.authorized(mockMvc,
                        post(PROCESS_URL_TEMPLATE.formatted(UUID.randomUUID()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"acao": "EXCLUIR"}
                                        """)))
                .andExpect(status().isNotFound());
    }
}
