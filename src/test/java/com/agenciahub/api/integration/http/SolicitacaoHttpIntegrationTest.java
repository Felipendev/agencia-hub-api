package com.agenciahub.api.integration.http;
import static com.agenciahub.api.support.IntegrationHttpSupport.unauthenticated;

import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SolicitacaoHttpIntegrationTest extends AbstractIntegrationTest {

    @Test
    void publicConfig_whenSlugUnknown_returns200WithDefaults() throws Exception {
        String slug = "inexistente-" + UUID.randomUUID();
        mockMvc.perform(get(IntegrationHttpSupport.API_PREFIX + "/public/solicitacao-config/" + slug))
                .andExpect(status().isOk());
    }

    @Test
    void publicSubmit_invalidBody_returns400() throws Exception {
        mockMvc.perform(post(IntegrationHttpSupport.API_PREFIX + "/public/solicitacao/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agencyConfig_get_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get(IntegrationHttpSupport.API_PREFIX + "/agency/solicitacao-config"))
                .andExpect(unauthenticated());
    }

    @Test
    void agencyConfig_get_withAuth_returns200() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/agency/solicitacao-config")))
                .andExpect(status().isOk());
    }

    @Test
    void agencyConfig_put_withoutAuth_returns401() throws Exception {
        mockMvc.perform(put(IntegrationHttpSupport.API_PREFIX + "/agency/solicitacao-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(unauthenticated());
    }

    @Test
    void agencyConfig_put_withAuth_returns200() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        put(IntegrationHttpSupport.API_PREFIX + "/agency/solicitacao-config")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "slug": "form-teste",
                                          "tituloPagina": "Solicitação de cotação"
                                        }
                                        """)))
                .andExpect(status().isOk());
    }

    @Test
    void submissions_list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get(IntegrationHttpSupport.API_PREFIX + "/agency/solicitacao-submissions"))
                .andExpect(unauthenticated());
    }

    @Test
    void submissions_list_withAuth_returns200() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        get(IntegrationHttpSupport.API_PREFIX + "/agency/solicitacao-submissions")))
                .andExpect(status().isOk());
    }

    @Test
    void submissions_delete_whenMissing_returns404() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        delete(IntegrationHttpSupport.API_PREFIX + "/agency/solicitacao-submissions/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void importedSubmission_isHiddenAndRemovedWhenQuotationIsDeleted() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String slug = "form-" + suffix;
        mockMvc.perform(http.authorized(
                        mockMvc,
                        put(IntegrationHttpSupport.API_PREFIX + "/agency/solicitacao-config")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"slug":"%s","tituloPagina":"Solicitação"}
                                        """.formatted(slug))))
                .andExpect(status().isOk());

        String submissionResponse = mockMvc.perform(post(IntegrationHttpSupport.API_PREFIX + "/public/solicitacao/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"%s","nome":"Lead de teste","email":"lead-%s@test.com",
                                 "telefone":"11999999999","detalhes":{"origem":"Recife"},"consentimentoLgpd":true}
                                """.formatted(slug, suffix)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID submissionId = UUID.fromString(http.parse(submissionResponse).get("id").asText());

        UUID customerId = createCustomerId(suffix);
        String quotationResponse = mockMvc.perform(http.authorized(
                        mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/quotations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"customerId":"%s","title":"Cotação importada","destination":"Recife",
                                         "totalAmount":100.00,"validUntil":"2026-12-31","publicSubmissionId":"%s"}
                                        """.formatted(customerId, submissionId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID quotationId = UUID.fromString(http.parse(quotationResponse).get("id").asText());

        assertSubmissionIsAbsent(submissionId);

        mockMvc.perform(http.authorized(
                        mockMvc,
                        delete(IntegrationHttpSupport.API_PREFIX + "/quotations/" + quotationId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(http.authorized(
                        mockMvc,
                        delete(IntegrationHttpSupport.API_PREFIX + "/agency/solicitacao-submissions/" + submissionId)))
                .andExpect(status().isNotFound());
    }

    private UUID createCustomerId(String suffix) throws Exception {
        String response = mockMvc.perform(http.authorized(
                        mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"name":"Cliente de teste","email":"customer-%s@test.com",
                                         "phone":"1198888%s","interestDestination":"Recife","status":"PROSPECT"}
                                        """.formatted(suffix, suffix.substring(0, 4)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(http.parse(response).get("id").asText());
    }

    private void assertSubmissionIsAbsent(UUID submissionId) throws Exception {
        String response = mockMvc.perform(http.authorized(
                        mockMvc,
                        get(IntegrationHttpSupport.API_PREFIX + "/agency/solicitacao-submissions")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (var node : http.parse(response)) {
            assertFalse(submissionId.toString().equals(node.path("id").asText()));
        }
    }
}
