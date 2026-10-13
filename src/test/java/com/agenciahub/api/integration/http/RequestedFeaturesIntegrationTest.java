package com.agenciahub.api.integration.http;

import com.agenciahub.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

class RequestedFeaturesIntegrationTest extends AbstractIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
    @org.springframework.beans.factory.annotation.Autowired private com.agenciahub.api.application.persistence.repository.AgencyRepository agencies;
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Test void agencyCannotReadEditDownloadOrDeleteOtherAgencyMaterials() throws Exception {
        var other=agencies.save(com.agenciahub.api.application.persistence.entity.Agency.builder().name("Outra agência").status(com.agenciahub.api.domain.AgencyStatus.ACTIVE).subscriptionStatus(com.agenciahub.api.domain.SubscriptionStatus.ACTIVE).build());
        var id=java.util.UUID.randomUUID();
        jdbc.update("INSERT INTO agency_library(id,agency_id,title,category,kind,body) VALUES(?,?,'Privado','OUTROS','TEMPLATE','Segredo')",id,other.getId());
        String jwt=http.loginOwner(mockMvc);
        mockMvc.perform(get("/library").header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '%s')]".formatted(id)).isEmpty());
        mockMvc.perform(get("/library/"+id+"/download").header("Authorization","Bearer "+jwt)).andExpect(status().isNotFound());
        mockMvc.perform(patch("/library/"+id).header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Tentativa\",\"kind\":\"TEMPLATE\",\"category\":\"OUTROS\",\"body\":\"alterado\"}")).andExpect(status().isNotFound());
        mockMvc.perform(delete("/library/"+id).header("Authorization","Bearer "+jwt)).andExpect(status().isNotFound());
    }
    @Test void fixedProfitCreatesTravelAndFinancialForecastAndSettlementDoesNotDuplicate() throws Exception {
        String jwt=http.loginOwner(mockMvc);
        String customer=mockMvc.perform(post("/customers").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON)
          .content("{\"name\":\"Cliente internacional\",\"phone\":\"+351912345678\",\"status\":\"PROSPECT\"}"))
          .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String customerId=http.parse(customer).path("id").asText();
        String body="""
          {"customerId":"%s","title":"Lucro fixo 170","destination":"Belém","totalAmount":1,"validUntil":"2027-12-31",
           "flightPlan":{"selectedOptionId":"flight","options":[{"id":"flight","nome":"Consolidadora","cia":"COPA + AZUL","qtdPessoas":1,
             "segmentos":[{"origin":"BPS","destination":"BEL","departureDate":"2025-10-25","arrivalDate":"2025-10-26","departureTime":"13:10","arrivalTime":"02:35","durationMinutes":805,"stops":1}],
             "calculo":{"tipo":"so_ida","milhasIda":0,"milhasVolta":0,"custoPorMilheiro":0,"taxas":1162.99,"taxasAdicionais":0,"valorMala":0,"qtdMalas":0,"revisado":true,"fonteTipo":"CONSOLIDADORA","empresaVendedora":"Pátria","observacoesComparativo":"5x no cartão",
             "lucroConfig":{"usarPct":false,"pct":0,"usarFixo":true,"fixo":170}}}]}}
          """.formatted(customerId);
        String quote=mockMvc.perform(post("/quotations").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isCreated()).andExpect(jsonPath("$.totalAmount").value(1332.99)).andReturn().getResponse().getContentAsString();
        String qid=http.parse(quote).path("id").asText();
        for(int i=0;i<2;i++)mockMvc.perform(patch("/quotations/"+qid).header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}" )).andExpect(status().isOk());
        String sales=mockMvc.perform(get("/sales").param("customerId",customerId).header("Authorization","Bearer "+jwt))
          .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].netProfit").value(170.0)).andReturn().getResponse().getContentAsString();
        String sid=http.parse(sales).get(0).path("id").asText();
        // Reproduce legacy records: no supplier cost, forecast or linked travel.
        String migration=new org.springframework.core.io.ClassPathResource("db/migration/V51__reconcile_quotation_flight_costs.sql").getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(tx -> {
            jdbc.update("DELETE FROM payables WHERE sale_id=?",java.util.UUID.fromString(sid));
            jdbc.update("DELETE FROM financial_entries WHERE customer_id=?",java.util.UUID.fromString(customerId));
            jdbc.update("UPDATE sale_items SET supplier_cost=NULL WHERE sale_id=?",java.util.UUID.fromString(sid));
            jdbc.update("DELETE FROM trips WHERE quotation_id=?",java.util.UUID.fromString(qid));
            jdbc.execute(migration); return null;
        });
        mockMvc.perform(get("/sales").param("customerId",customerId).header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$[0].netProfit").value(170.0));
        mockMvc.perform(get("/trips").param("customerId",customerId).header("Authorization","Bearer "+jwt))
          .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].travelStartDate").value("2025-10-25"));
        String detail=mockMvc.perform(get("/sales/"+sid).header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String rid=http.parse(detail).path("receivables").get(0).path("id").asText();
        String pid=http.parse(detail).path("payables").get(0).path("id").asText();
        String financial=mockMvc.perform(get("/financial-entries").param("customerId",customerId).header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(http.parse(financial).size()).isEqualTo(2);
        String entryId=http.parse(financial).get(0).path("id").asText();
        mockMvc.perform(delete("/financial-entries/"+entryId).header("Authorization","Bearer "+jwt)).andExpect(status().isBadRequest());
        mockMvc.perform(post("/sales/payables/"+pid+"/settle").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isOk());
        mockMvc.perform(post("/sales/payables/"+pid+"/unsettle").header("Authorization","Bearer "+jwt)).andExpect(status().isOk());
        mockMvc.perform(post("/sales/receivables/"+rid+"/settle").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isOk());
        mockMvc.perform(get("/financial-entries").param("customerId",customerId).header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(post("/sales/receivables/"+rid+"/settle").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isBadRequest());
        mockMvc.perform(post("/sales/receivables/"+rid+"/unsettle").header("Authorization","Bearer "+jwt)).andExpect(status().isOk());
        mockMvc.perform(patch("/quotations/"+qid).header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DRAFT\"}" )).andExpect(status().isOk());
    }
    @Test void libraryTemplatesFilesAndUnsafeLinks() throws Exception {
        String jwt=http.loginOwner(mockMvc);
        String result=mockMvc.perform(post("/library").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON)
          .content("{\"title\":\"NF modelo\",\"category\":\"NOTA_FISCAL\",\"kind\":\"TEMPLATE\",\"body\":\"Cliente: [nome]\"}"))
          .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id=http.parse(result).path("id").asText();
        mockMvc.perform(patch("/library/"+id).header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON)
          .content("{\"title\":\"NF editada\",\"category\":\"NOTA_FISCAL\",\"kind\":\"TEMPLATE\",\"body\":\"Cliente: Maria\"}"))
          .andExpect(status().isOk()).andExpect(jsonPath("$.body").value("Cliente: Maria"));
        mockMvc.perform(post("/library").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON)
          .content("{\"title\":\"Inválido\",\"category\":\"OUTROS\",\"kind\":\"LINK\",\"url\":\"javascript:alert(1)\"}"))
          .andExpect(status().isBadRequest());
        MockMultipartFile file=new MockMultipartFile("file","contrato.txt","text/plain","Contrato da agência".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String upload=mockMvc.perform(multipart("/library/files").file(file).param("title","Contrato").param("category","CONTRATO").header("Authorization","Bearer "+jwt))
          .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String fid=http.parse(upload).path("id").asText();
        mockMvc.perform(get("/library/"+fid+"/download").header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(content().bytes(file.getBytes()));
        mockMvc.perform(delete("/library/"+id).header("Authorization","Bearer "+jwt)).andExpect(status().isNoContent());
        mockMvc.perform(patch("/library/"+id).header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(result)).andExpect(status().isNotFound());
    }
}
