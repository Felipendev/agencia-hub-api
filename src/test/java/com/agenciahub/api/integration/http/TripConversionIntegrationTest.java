package com.agenciahub.api.integration.http;

import com.agenciahub.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Regression: quotation conversion and retries must not create duplicate trips. */
class TripConversionIntegrationTest extends AbstractIntegrationTest {
    @Test void competingConversionsAndLaterApprovalReuseOneTripWhileOtherServicesRemainAllowed() throws Exception {
        String jwt=http.loginOwner(mockMvc);
        String customer=mockMvc.perform(post("/customers").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Cliente conversão concorrente\",\"status\":\"PROSPECT\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String customerId=http.parse(customer).path("id").asText();
        String quotation=mockMvc.perform(post("/quotations").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON)
            .content("{\"customerId\":\"%s\",\"title\":\"Concorrência\",\"destination\":\"Lisboa\",\"totalAmount\":1000,\"validUntil\":\"2027-12-31\"}".formatted(customerId)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String quoteId=http.parse(quotation).path("id").asText();
        String body="{\"customerId\":\"%s\",\"quotationId\":\"%s\",\"serviceType\":\"FLIGHT\",\"bookingLocator\":\"COMMON\"}".formatted(customerId,quoteId);
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Integer> submit=()->mockMvc.perform(post("/trips").header("Authorization","Bearer "+jwt).header("Idempotency-Key",java.util.UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getStatus();
            var results=executor.invokeAll(java.util.List.of(submit,submit));
            org.assertj.core.api.Assertions.assertThat(java.util.List.of(results.get(0).get(),results.get(1).get())).containsExactlyInAnyOrder(201,409);
        } finally {executor.shutdownNow();}
        mockMvc.perform(patch("/quotations/"+quoteId).header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/trips").param("quotationId",quoteId).header("Authorization","Bearer "+jwt)).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(post("/trips").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(body.replace("FLIGHT","HOTEL")))
            .andExpect(status().isCreated());
        mockMvc.perform(get("/trips").param("quotationId",quoteId).header("Authorization","Bearer "+jwt)).andExpect(jsonPath("$.length()").value(2));
    }
    @Test void concurrentRetriesReuseOneTripAndChangedPayloadIsRejected() throws Exception {
        String jwt=http.loginOwner(mockMvc);
        String customer=mockMvc.perform(post("/customers").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Cliente reenvio\",\"status\":\"PROSPECT\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String customerId=http.parse(customer).path("id").asText();
        String body="{\"customerId\":\"%s\",\"serviceType\":\"FLIGHT\",\"bookingLocator\":\"RETRY1\"}".formatted(customerId);
        String key=java.util.UUID.randomUUID().toString();
        java.util.concurrent.ExecutorService executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<String> submit=()->mockMvc.perform(post("/trips").header("Authorization","Bearer "+jwt).header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            var results=executor.invokeAll(java.util.List.of(submit,submit));
            String first=http.parse(results.get(0).get()).path("trip").path("id").asText();
            String second=http.parse(results.get(1).get()).path("trip").path("id").asText();
            org.assertj.core.api.Assertions.assertThat(first).isEqualTo(second);
        } finally { executor.shutdownNow(); }
        mockMvc.perform(get("/trips").param("customerId",customerId).header("Authorization","Bearer "+jwt)).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(post("/trips").header("Authorization","Bearer "+jwt).header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON).content(body.replace("RETRY1","CHANGED")))
            .andExpect(status().isConflict());
    }
    @Test void approvalThenManualConversionAndRetryKeepOneTrip() throws Exception {
        String jwt=http.loginOwner(mockMvc);
        String customer=mockMvc.perform(post("/customers").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Cliente diagnóstico duplicação\",\"status\":\"PROSPECT\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String customerId=http.parse(customer).path("id").asText();
        String quote=mockMvc.perform(post("/quotations").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"customerId":"%s","title":"Diagnóstico conversão","destination":"Belém","totalAmount":1332.99,"status":"ACCEPTED","validUntil":"2027-12-31","travelStartDate":"2027-10-25","travelEndDate":"2027-10-26"}
                """.formatted(customerId)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String quoteId=http.parse(quote).path("id").asText();
        mockMvc.perform(get("/trips").param("quotationId",quoteId).header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/trips").param("customerId",customerId).header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        String manual="""
            {"customerId":"%s","quotationId":"%s","serviceType":"FLIGHT","bookingLocator":"TESTPNR","travelStartDate":"2027-10-25","travelEndDate":"2027-10-26","notes":"Conversão manual do mesmo orçamento"}
            """.formatted(customerId,quoteId);
        for(int i=0;i<2;i++)mockMvc.perform(post("/trips").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(manual))
            .andExpect(status().isConflict());
        mockMvc.perform(get("/trips").param("customerId",customerId).header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/sales").param("customerId",customerId).header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        // Reapplying approval preserves the single linked trip.
        mockMvc.perform(patch("/quotations/"+quoteId).header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/trips").param("customerId",customerId).header("Authorization","Bearer "+jwt)).andExpect(jsonPath("$.length()").value(1));
        String standalone=mockMvc.perform(post("/trips").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON)
            .content("{\"customerId\":\"%s\",\"serviceType\":\"FLIGHT\"}".formatted(customerId)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String standaloneId=http.parse(standalone).path("trip").path("id").asText();
        mockMvc.perform(patch("/trips/"+standaloneId).header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(manual))
            .andExpect(status().isConflict());
    }
}
