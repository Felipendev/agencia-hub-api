package com.agenciahub.api.integration.http;

import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static com.agenciahub.api.support.IntegrationHttpSupport.unauthenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerHttpIntegrationTest extends AbstractIntegrationTest {

    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get(IntegrationHttpSupport.API_PREFIX + "/customers"))
                .andExpect(unauthenticated());
    }

    @Test
    void list_withAuth_returns200() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/customers")))
                .andExpect(status().isOk());
    }

    @Test
    void lookup_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get(IntegrationHttpSupport.API_PREFIX + "/customers/lookup")
                        .param("email", "x@test.com"))
                .andExpect(unauthenticated());
    }

    @Test
    void lookup_withAuth_unknown_returns404() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        get(IntegrationHttpSupport.API_PREFIX + "/customers/lookup")
                                .param("email", "naoexiste-" + UUID.randomUUID() + "@test.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_whenMissing_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/customers/" + id)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void create_happyPath_returns201() throws Exception {
        String email = "crm-" + UUID.randomUUID() + "@integration.test";
        mockMvc.perform(http.authorized(
                        mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Cliente Integração",
                                          "email": "%s",
                                          "phone": "11999990000",
                                          "interestDestination": "Europa",
                                          "status": "PROSPECT"
                                        }
                                        """.formatted(email))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void create_withoutEmailAndPhone_returns201() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Cliente sem contato",
                                          "interestDestination": "Europa",
                                          "status": "PROSPECT"
                                        }
                                        """)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").isEmpty())
                .andExpect(jsonPath("$.phone").isEmpty());
    }

    @Test
    void create_invalidEmail_returns400() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "X",
                                          "email": "invalido",
                                          "phone": "11999990000",
                                          "interestDestination": "Europa",
                                          "status": "PROSPECT"
                                        }
                                        """)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void patch_whenMissing_returns404() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        patch(IntegrationHttpSupport.API_PREFIX + "/customers/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Novo\"}")))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_whenMissing_returns404() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        delete(IntegrationHttpSupport.API_PREFIX + "/customers/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }
}
