package com.agenciahub.api.integration.http;

import com.agenciahub.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP and fresh database reads: commercial margins must not invent agency cash obligations. */
class CommercialFlowIntegrationTest extends AbstractIntegrationTest {
    @Autowired private com.agenciahub.api.application.persistence.repository.AgencyRepository agencies;
    @Autowired private com.agenciahub.api.application.persistence.repository.SupplierRepository suppliers;
    private String create(String token, String path, String body) throws Exception {
        return http.parse(mockMvc.perform(post(path).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asText();
    }

    private String customer(String token) throws Exception {
        return create(token, "/customers", "{\"name\":\"Commercial test\",\"status\":\"PROSPECT\"}");
    }

    private String sale(String token, String customerId, String supplierId, String cost, boolean direct) throws Exception {
        String body = """
                {"customerId":"%s","saleDate":"2026-10-28","totalAmount":1000,
                 "notes":"Não confundir margem com caixa",
                 "items":[{"supplierId":%s,"description":"Hotel","itemType":"HOTEL",
                           "saleAmount":1000,"supplierCost":%s,"customerPaysSupplierDirectly":%s}],
                 "receivables":%s}
                """.formatted(customerId, supplierId == null ? "null" : "\"" + supplierId + "\"", cost, direct,
                direct ? "[]" : "[{\"number\":1,\"amount\":1000,\"dueDate\":\"2026-11-01\"}]");
        return create(token, "/sales", body);
    }

    @Test void directSupplierPaymentKeepsMarginWithoutAgencyPayableOrCash() throws Exception {
        String token = http.loginOwner(mockMvc), customer = customer(token);
        String id = sale(token, customer, null, "800", true);
        mockMvc.perform(get("/sales/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.grossProfit").value(200))
                .andExpect(jsonPath("$.netProfit").value(200))
                .andExpect(jsonPath("$.receivables.length()").value(0))
                .andExpect(jsonPath("$.payables.length()").value(0))
                .andExpect(jsonPath("$.items[0].customerPaysSupplierDirectly").value(true));
        mockMvc.perform(get("/financial-entries").param("customerId", customer).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test void unknownSupplierCostHasUnavailableMarginInSummaryAndDetails() throws Exception {
        String token = http.loginOwner(mockMvc), customer = customer(token);
        String id = sale(token, customer, null, "null", false);
        mockMvc.perform(get("/sales").param("customerId", customer).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].grossProfit").isEmpty())
                .andExpect(jsonPath("$[0].netProfit").isEmpty());
        mockMvc.perform(get("/sales/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.grossProfit").isEmpty())
                .andExpect(jsonPath("$.netProfit").isEmpty());
    }

    @Test void supplierCrudPersistsAndRejectsBlankOrDuplicateNamesOnEdit() throws Exception {
        String token = http.loginOwner(mockMvc), name = "Fornecedor " + UUID.randomUUID();
        String id = create(token, "/suppliers", "{\"name\":\"" + name + "\",\"phone\":\"+351912345678\"}");
        String second = create(token, "/suppliers", "{\"name\":\"Outro " + name + "\"}");
        mockMvc.perform(patch("/suppliers/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/suppliers/" + second).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + name.toUpperCase() + "\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/suppliers/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"Atualizado\",\"contactName\":\"Contato\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mockMvc.perform(get("/suppliers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '%s')].notes".formatted(id)).value("Atualizado"))
                .andExpect(jsonPath("$[?(@.id == '%s')].phone".formatted(id)).value("+351912345678"));
        mockMvc.perform(delete("/suppliers/" + id).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        mockMvc.perform(patch("/suppliers/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"Não pode voltar\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/suppliers/" + second).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
    }

    @Test void foreignSupplierCannotBeListedEditedDeletedOrUsedInASale() throws Exception {
        var agency = agencies.save(com.agenciahub.api.application.persistence.entity.Agency.builder()
                .name("Outra agência comercial").status(com.agenciahub.api.domain.AgencyStatus.ACTIVE)
                .subscriptionStatus(com.agenciahub.api.domain.SubscriptionStatus.ACTIVE).build());
        var supplier = suppliers.save(com.agenciahub.api.application.persistence.entity.Supplier.builder()
                .agency(agency).name("Privado").notes("Não compartilhar").build());
        String token = http.loginOwner(mockMvc), customer = customer(token);
        mockMvc.perform(get("/suppliers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '%s')]".formatted(supplier.getId())).isEmpty());
        mockMvc.perform(patch("/suppliers/" + supplier.getId()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Tentativa\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/suppliers/" + supplier.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/sales").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","saleDate":"2026-10-28","totalAmount":1000,
                                 "items":[{"supplierId":"%s","description":"Privado","saleAmount":1000,"supplierCost":800}],
                                 "receivables":[{"number":1,"amount":1000,"dueDate":"2026-11-01"}]}
                                """.formatted(customer, supplier.getId())))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/sales").param("customerId", customer).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
}
