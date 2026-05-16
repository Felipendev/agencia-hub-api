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
}
