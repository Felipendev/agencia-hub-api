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
    void create_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post(IntegrationHttpSupport.API_PREFIX + "/quotations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "title": "Teste",
                                  "destination": "Lisboa",
                                  "totalAmount": 100.00,
                                  "validUntil": "2026-12-31"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(unauthenticated());
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
    void create_withNegativeTotalAmount_returns400() throws Exception {
        UUID customerId = createCustomerId();
        mockMvc.perform(http.authorized(
                        mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/quotations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "customerId": "%s",
                                          "title": "Teste",
                                          "destination": "Lisboa",
                                          "totalAmount": -50.00,
                                          "validUntil": "2026-12-31"
                                        }
                                        """.formatted(customerId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_happyPath_returns201() throws Exception {
        UUID customerId = createCustomerId();
        mockMvc.perform(http.authorized(
                        mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/quotations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "customerId": "%s",
                                          "title": "Pacote Europa",
                                          "destination": "Lisboa",
                                          "totalAmount": 4500.00,
                                          "validUntil": "2026-12-31"
                                        }
                                        """.formatted(customerId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Pacote Europa"))
                .andExpect(jsonPath("$.totalAmount").value(4500.00))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()));
    }

    @Test
    void createAndGet_happyPath_returns200() throws Exception {
        UUID customerId = createCustomerId();
        UUID quotationId = createQuotationId(customerId, "Viagem Japão", 8000.00);

        mockMvc.perform(http.authorized(mockMvc,
                        get(IntegrationHttpSupport.API_PREFIX + "/quotations/" + quotationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(quotationId.toString()))
                .andExpect(jsonPath("$.title").value("Viagem Japão"))
                .andExpect(jsonPath("$.totalAmount").value(8000.00));
    }

    @Test
    void createAndPatch_updatesTitle_returns200() throws Exception {
        UUID customerId = createCustomerId();
        UUID quotationId = createQuotationId(customerId, "Título Original", 1000.00);

        mockMvc.perform(http.authorized(
                        mockMvc,
                        patch(IntegrationHttpSupport.API_PREFIX + "/quotations/" + quotationId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"Título Atualizado\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Título Atualizado"))
                .andExpect(jsonPath("$.totalAmount").value(1000.00));
    }

    @Test
    void createAndDelete_returns204ThenGone() throws Exception {
        UUID customerId = createCustomerId();
        UUID quotationId = createQuotationId(customerId, "Para Deletar", 500.00);

        mockMvc.perform(http.authorized(mockMvc,
                        delete(IntegrationHttpSupport.API_PREFIX + "/quotations/" + quotationId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(http.authorized(mockMvc,
                        get(IntegrationHttpSupport.API_PREFIX + "/quotations/" + quotationId)))
                .andExpect(status().isNotFound());
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

    private UUID createCustomerId() throws Exception {
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 9);
        String email = "customer-" + unique + "@quotation.test";
        String phone = "119" + unique;
        String response = mockMvc.perform(http.authorized(
                        mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Cliente Cotação",
                                          "email": "%s",
                                          "phone": "%s",
                                          "interestDestination": "Europa",
                                          "status": "PROSPECT"
                                        }
                                        """.formatted(email, phone))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(http.parse(response).get("id").asText());
    }

    private UUID createQuotationId(UUID customerId, String title, double totalAmount) throws Exception {
        String response = mockMvc.perform(http.authorized(
                        mockMvc,
                        post(IntegrationHttpSupport.API_PREFIX + "/quotations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "customerId": "%s",
                                          "title": "%s",
                                          "destination": "Lisboa",
                                          "totalAmount": %s,
                                          "validUntil": "2026-12-31"
                                        }
                                        """.formatted(customerId, title, totalAmount))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(http.parse(response).get("id").asText());
    }
}
