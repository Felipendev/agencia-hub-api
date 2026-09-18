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

    @Test
    void flightPlanPersistsAndRecalculationKeepsPreviousSnapshot() throws Exception {
        UUID customerId = createCustomerId();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var plan = mapper.readTree("""
                {"selectedOptionId":"flight","options":[{"id":"flight","nome":"Voo","cia":"LATAM","qtdPessoas":1,
                  "segmentos":[{"origin":"BPS","destination":"REC","departureDate":"2026-09-20","arrivalDate":"2026-09-20",
                    "departureTime":"17:25","arrivalTime":"23:20","durationMinutes":355,"stops":1}],
                  "calculo":{"tipo":"so_ida","milhasIda":20000,"milhasVolta":0,"custoPorMilheiro":25,"taxas":50,
                    "taxasAdicionais":0,"valorMala":0,"qtdMalas":0,"revisado":true,
                    "lucroConfig":{"usarPct":true,"pct":10,"usarFixo":false,"fixo":0}}}]}
                """);
        var body = mapper.createObjectNode().put("customerId", customerId.toString()).put("title", "Importada")
                .put("destination", "Recife").put("totalAmount", 1).put("validUntil", "2026-12-31");
        body.set("flightPlan", plan);
        String created = mockMvc.perform(http.authorized(mockMvc, post("/quotations")
                .contentType(MediaType.APPLICATION_JSON).content(body.toString())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.totalAmount").value(605.0))
                .andReturn().getResponse().getContentAsString();
        String id = http.parse(created).path("id").asText();
        mockMvc.perform(http.authorized(mockMvc, get("/quotations/" + id)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.flightPlan.options[0].calculo.milhasIda").value(20000));
        ((com.fasterxml.jackson.databind.node.ObjectNode) plan.path("options").get(0).path("calculo")).put("taxasAdicionais", 10);
        var patchBody = mapper.createObjectNode(); patchBody.set("flightPlan", plan);
        mockMvc.perform(http.authorized(mockMvc, patch("/quotations/" + id)
                .contentType(MediaType.APPLICATION_JSON).content(patchBody.toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(616.0));
        mockMvc.perform(http.authorized(mockMvc, get("/quotations/" + id + "/flight-history")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].options[0].precoTotal").value(605.0));
        mockMvc.perform(http.authorized(mockMvc, patch("/quotations/" + id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"totalAmount\":1}")))
                .andExpect(status().isBadRequest());
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
