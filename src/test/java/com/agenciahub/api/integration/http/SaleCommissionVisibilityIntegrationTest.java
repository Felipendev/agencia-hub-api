package com.agenciahub.api.integration.http;

import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uma comissão anexada a um usuário numa venda precisa aparecer no painel financeiro
 * ("Minhas Comissões") desse mesmo usuário — `GET /sales/commissions/mine`.
 */
class SaleCommissionVisibilityIntegrationTest extends AbstractIntegrationTest {

    @Test
    void commissionAttachedToUser_appearsInTheirOwnCommissionsList() throws Exception {
        String token = http.loginOwner(mockMvc);
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        String recipientId = http.parse(mockMvc.perform(get(IntegrationHttpSupport.API_PREFIX + "/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())
                .get(0).get("id").asText();

        String customerId = http.parse(mockMvc.perform(post(IntegrationHttpSupport.API_PREFIX + "/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cliente comissão " + suffix + "\",\"status\":\"PROSPECT\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString())
                .get("id").asText();

        String body = """
                {
                  "customerId": "%s",
                  "totalAmount": 1000.00,
                  "saleDate": "%s",
                  "receivables": [{"number": 1, "amount": 1000.00, "dueDate": "%s"}],
                  "commissions": [{"recipientUserId": "%s", "calculationType": "PERCENTAGE", "calculationValue": 10}]
                }
                """.formatted(customerId, LocalDate.now(), LocalDate.now().plusDays(10), recipientId);

        mockMvc.perform(post(IntegrationHttpSupport.API_PREFIX + "/sales")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get(IntegrationHttpSupport.API_PREFIX + "/sales/commissions/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.customerId == '%s')].amount".formatted(customerId)).value(100.0))
                .andExpect(jsonPath("$[?(@.customerId == '%s')].status".formatted(customerId)).value("PENDING"));
    }
}
