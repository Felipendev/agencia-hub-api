package com.agenciahub.api.integration.http;
import static com.agenciahub.api.support.IntegrationHttpSupport.unauthenticated;

import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuotationHttpIntegrationTest extends AbstractIntegrationTest {

    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get(IntegrationHttpSupport.API_PREFIX + "/quotations"))
                .andExpect(unauthenticated());
    }

    @Test
    void list_withAuth_returns200() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/quotations")))
                .andExpect(status().isOk());
    }

    @Test
    void get_whenMissing_returns404() throws Exception {
        mockMvc.perform(http.authorized(mockMvc, get(IntegrationHttpSupport.API_PREFIX + "/quotations/" + UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void create_whenCustomerMissing_returns404() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/quotations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "customerId": "%s",
                                          "title": "Teste",
                                          "destination": "Lisboa",
                                          "totalAmount": 100.00,
                                          "validUntil": "2026-12-31"
                                        }
                                        """.formatted(UUID.randomUUID()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void patch_whenMissing_returns404() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        patch(IntegrationHttpSupport.API_PREFIX + "/quotations/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"X\"}")))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_whenMissing_returns404() throws Exception {
        mockMvc.perform(http.authorized(
                        mockMvc,
                        delete(IntegrationHttpSupport.API_PREFIX + "/quotations/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }
}
