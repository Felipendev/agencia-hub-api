package com.agenciahub.api.integration.http;

import com.agenciahub.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

class LibraryAndAccumulationIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired com.agenciahub.api.application.persistence.repository.AgencyRepository agencies;
    private static final String SCENARIO="""
        {"title":"Meta Lisboa","monthlySpending":10000,"pointsPerDollar":2,"exchangeRate":5,
         "exchangeDate":"2026-01-01","months":12,"passengers":2,"milesPerPassenger":40000,
         "destination":"Lisboa","scenarios":[{"program":"Smiles","bonusPercent":100},{"program":"LATAM","bonusPercent":30}]}
        """;

    @Test void defaultLinksCanBeEditedAndDeletedWithoutReturningOnReload() throws Exception {
        String jwt=http.loginOwner(mockMvc);
        String first=mockMvc.perform(get("/library").header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var link=java.util.stream.StreamSupport.stream(http.parse(first).spliterator(),false).filter(r->r.path("title").asText().equals("LATAM — bagagem")).findFirst().orElseThrow();
        String id=link.path("id").asText();
        mockMvc.perform(patch("/library/"+id).header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Bagagem revisada\",\"category\":\"BAGAGEM\",\"kind\":\"LINK\",\"url\":\"https://www.latamairlines.com/\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Bagagem revisada"));
        mockMvc.perform(delete("/library/"+id).header("Authorization","Bearer "+jwt)).andExpect(status().isNoContent());
        for(int i=0;i<2;i++) mockMvc.perform(get("/library").header("Authorization","Bearer "+jwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(id)).isEmpty()).andExpect(jsonPath("$[?(@.title == 'LATAM — bagagem')]").isEmpty());
    }
    @Test void editingFileReplacesDownloadAndPreservesResourceId() throws Exception {
        String jwt=http.loginOwner(mockMvc);
        var file=new MockMultipartFile("file","contrato.txt","text/plain","Primeira versão".getBytes(StandardCharsets.UTF_8));
        String created=mockMvc.perform(multipart("/library/files").file(file).param("title","Contrato").param("category","CONTRATO").header("Authorization","Bearer "+jwt))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id=http.parse(created).path("id").asText();
        var replacement=new MockMultipartFile("file","novo.txt","text/plain","Documento revisado".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/library/"+id+"/file").file(replacement).param("title","Contrato revisado").param("category","CONTRATO").with(r->{r.setMethod("PUT");return r;}).header("Authorization","Bearer "+jwt))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.filename").value("novo.txt"));
        mockMvc.perform(get("/library/"+id+"/download").header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(content().bytes("Documento revisado".getBytes(StandardCharsets.UTF_8)));
    }
    @Test void savedScenarioRecalculatesAndIsIsolatedFromOtherAgencies() throws Exception {
        String jwt=http.loginOwner(mockMvc);
        String created=mockMvc.perform(post("/accumulation-simulations").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(SCENARIO))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.result.monthlyPoints").value(4000)).andExpect(jsonPath("$.result.basePoints").value(48000))
            .andExpect(jsonPath("$.result.goalMiles").value(80000)).andExpect(jsonPath("$.result.scenarios[0].projectedMiles").value(96000))
            .andExpect(jsonPath("$.result.scenarios[1].remainingMiles").value(17600)).andReturn().getResponse().getContentAsString();
        String id=http.parse(created).path("id").asText();
        mockMvc.perform(get("/accumulation-simulations").header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '%s')].assumptions.destination".formatted(id)).value("Lisboa"));
        var other=agencies.save(com.agenciahub.api.application.persistence.entity.Agency.builder().name("Outra simulação").status(com.agenciahub.api.domain.AgencyStatus.ACTIVE).subscriptionStatus(com.agenciahub.api.domain.SubscriptionStatus.ACTIVE).build());
        UUID privateId=UUID.randomUUID();
        jdbc.update("INSERT INTO accumulation_simulations(id,agency_id,title,assumptions,created_by) VALUES(?,?,?,?::jsonb,?)",privateId,other.getId(),"Privada",SCENARIO,"teste");
        mockMvc.perform(get("/accumulation-simulations").header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '%s')]".formatted(privateId)).isEmpty());
        mockMvc.perform(delete("/accumulation-simulations/"+privateId).header("Authorization","Bearer "+jwt)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM accumulation_simulations WHERE id=?",Integer.class,privateId)).isEqualTo(1);
        mockMvc.perform(delete("/accumulation-simulations/"+id).header("Authorization","Bearer "+jwt)).andExpect(status().isNoContent());
        mockMvc.perform(get("/accumulation-simulations").header("Authorization","Bearer "+jwt)).andExpect(jsonPath("$[?(@.id == '%s')]".formatted(id)).isEmpty());
    }
    @Test void invalidExchangeAndPeriodsAreRejectedAndAuthenticationIsRequired() throws Exception {
        String jwt=http.loginOwner(mockMvc);
        for(String invalid:new String[]{SCENARIO.replace("\"exchangeRate\":5","\"exchangeRate\":0"),SCENARIO.replace("\"months\":12","\"months\":0"),SCENARIO.replace("\"bonusPercent\":100","\"bonusPercent\":-1")})
            mockMvc.perform(post("/accumulation-simulations").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/accumulation-simulations")).andExpect(com.agenciahub.api.support.IntegrationHttpSupport.unauthenticated());
    }
}
