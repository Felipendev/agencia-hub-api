package com.agenciahub.api.integration.http;

import com.agenciahub.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

class AccumulationSimulationCrudIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired com.agenciahub.api.application.persistence.repository.AgencyRepository agencies;
    private static final String INPUT="""
        {"title":"Planejamento inicial","monthlySpending":10000,"pointsPerDollar":2,"exchangeRate":5,
        "exchangeDate":"2026-01-01","months":12,"passengers":2,"milesPerPassenger":40000,"destination":"Lisboa",
        "scenarios":[{"program":"Smiles","bonusPercent":100}]}
        """;
    private String create(String jwt) throws Exception {
        String customer=mockMvc.perform(post("/customers").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Plano "+UUID.randomUUID()+"\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String payload="{\"customerId\":\""+http.parse(customer).path("id").asText()+"\",\"assumptions\":"+INPUT+"}";
        String response=mockMvc.perform(post("/accumulation-simulations").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(1)).andReturn().getResponse().getContentAsString();
        return http.parse(response).path("id").asText();
    }
    @Test void editPreservesIdAndHistoryAndDeleteRemovesVersions() throws Exception {
        String jwt=http.loginOwner(mockMvc),id=create(jwt);
        String changed=INPUT.replace("10000","20000").replace("Planejamento inicial","Planejamento atualizado");
        String patch="{\"assumptions\":%s,\"expectedVersion\":1}".formatted(changed);
        mockMvc.perform(patch("/accumulation-simulations/"+id).header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(patch))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.version").value(2))
            .andExpect(jsonPath("$.result.basePoints").value(96000)).andExpect(jsonPath("$.result.scenarios[0].projectedMiles").value(192000));
        // A stale retry is a conflict, even when the numerical inputs happen to match.
        mockMvc.perform(patch("/accumulation-simulations/"+id).header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(patch))
            .andExpect(status().isConflict());
        mockMvc.perform(get("/accumulation-simulations/"+id).header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$.assumptions.title").value("Planejamento atualizado"));
        mockMvc.perform(get("/accumulation-simulations/"+id+"/history").header("Authorization","Bearer "+jwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].version").value(2)).andExpect(jsonPath("$[0].author").isNotEmpty())
            .andExpect(jsonPath("$[1].assumptions.monthlySpending").value(10000)).andExpect(jsonPath("$[1].result.basePoints").value(48000));
        mockMvc.perform(delete("/accumulation-simulations/"+id).header("Authorization","Bearer "+jwt)).andExpect(status().isNoContent());
        mockMvc.perform(get("/accumulation-simulations/"+id).header("Authorization","Bearer "+jwt)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM accumulation_simulation_versions WHERE simulation_id=?",Integer.class,UUID.fromString(id))).isZero();
    }
    @Test void concurrentEditorsDoNotOverwriteEachOther() throws Exception {
        String jwt=http.loginOwner(mockMvc),id=create(jwt);
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.List<java.util.concurrent.Callable<Integer>> edits=new java.util.ArrayList<>();
            for(String title:java.util.List.of("Edição A","Edição B"))edits.add(()->mockMvc.perform(patch("/accumulation-simulations/"+id).header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON)
                .content("{\"assumptions\":%s,\"expectedVersion\":1}".formatted(INPUT.replace("Planejamento inicial",title)))).andReturn().getResponse().getStatus());
            var results=executor.invokeAll(edits);
            assertThat(java.util.List.of(results.get(0).get(),results.get(1).get())).containsExactlyInAnyOrder(200,409);
        }finally{executor.shutdownNow();}
        mockMvc.perform(get("/accumulation-simulations/"+id+"/history").header("Authorization","Bearer "+jwt)).andExpect(jsonPath("$.length()").value(2));
    }
    @Test void anotherAgencyCannotReadHistoryEditOrDeleteSimulation() throws Exception {
        String jwt=http.loginOwner(mockMvc);
        var other=agencies.save(com.agenciahub.api.application.persistence.entity.Agency.builder().name("Agência privada CRUD").status(com.agenciahub.api.domain.AgencyStatus.ACTIVE).subscriptionStatus(com.agenciahub.api.domain.SubscriptionStatus.ACTIVE).build());
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO accumulation_simulations(id,agency_id,title,assumptions,created_by) VALUES(?,?,?,?::jsonb,?)",id,other.getId(),"Privada",INPUT,"teste");
        mockMvc.perform(get("/accumulation-simulations/"+id).header("Authorization","Bearer "+jwt)).andExpect(status().isNotFound());
        mockMvc.perform(get("/accumulation-simulations/"+id+"/history").header("Authorization","Bearer "+jwt)).andExpect(status().isNotFound());
        mockMvc.perform(patch("/accumulation-simulations/"+id).header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content("{\"assumptions\":%s,\"expectedVersion\":1}".formatted(INPUT))).andExpect(status().isNotFound());
        mockMvc.perform(delete("/accumulation-simulations/"+id).header("Authorization","Bearer "+jwt)).andExpect(status().isNotFound());
    }
}
