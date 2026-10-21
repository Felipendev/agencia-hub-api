package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.*;
import com.agenciahub.api.application.persistence.repository.*;
import com.agenciahub.api.domain.*;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.scheduling.AgencyDeletionScheduler;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TripStagesLifecycleIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AgencyRepository agencies;
    @Autowired CrmCustomerRepository customers;
    @Autowired PlatformAccountRepository accounts;
    @Autowired AgencyDeletionScheduler scheduler;

    private static final String INPUT = """
            {"kind":"HOTEL","title":"Hotel Central","startsAt":"2099-01-10T14:00:00-03:00",
             "endsAt":"2099-01-12T12:00:00-03:00","timeZone":"America/Fortaleza","provider":"Rede X",
             "confirmationCode":"ABC","location":"Centro","people":2,"notes":"Quarto duplo"}
            """;
    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder r, String value) {
        return r.contentType(MediaType.APPLICATION_JSON).content(value);
    }
    private JsonNode call(String jwt, MockHttpServletRequestBuilder r, int code) throws Exception {
        String value=mockMvc.perform(r.header("Authorization","Bearer "+jwt)).andExpect(status().is(code)).andReturn().getResponse().getContentAsString();
        return value.isEmpty()?null:http.parse(value);
    }
    private JsonNode trip(String jwt) throws Exception {
        String person=call(jwt,body(post("/customers"),"{\"name\":\"Etapas revisão "+UUID.randomUUID()+"\",\"status\":\"ACTIVE\"}"),201).path("id").asText();
        return call(jwt,body(post("/trips"),"{\"customerId\":\""+person+"\",\"serviceType\":\"AEREO\",\"status\":\"UPCOMING\"}"),201).path("trip");
    }

    @Test void retryChecksAllFieldsAndSameStatusChecksVersion() throws Exception {
        String jwt=http.loginOwner(mockMvc),trip=trip(jwt).path("id").asText(),base="/trips/"+trip+"/stages";
        UUID key=UUID.randomUUID();
        call(jwt,body(post(base).header("Idempotency-Key",key),INPUT),201);
        for(String changed:java.util.List.of(INPUT.replace("2099-01-12T12","2099-01-13T12"),INPUT.replace("Fortaleza","Sao_Paulo"),
                INPUT.replace("Rede X","Rede Y"),INPUT.replace("ABC","DEF"),INPUT.replace("Centro","Praia"),
                INPUT.replace("\"people\":2","\"people\":3"),INPUT.replace("Quarto duplo","Suíte"))) {
            call(jwt,body(post(base).header("Idempotency-Key",key),changed),409);
        }
        call(jwt,body(post(base).header("Idempotency-Key",key),INPUT.replace("Hotel Central","  Hotel Central  ")),201);
        call(jwt,body(post(base+"/"+key+"/status"),"{\"status\":\"CONFIRMED\",\"expectedVersion\":1}"),200);
        call(jwt,body(post(base+"/"+key+"/status"),"{\"status\":\"CONFIRMED\",\"expectedVersion\":1}"),409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM trip_stage_events WHERE stage_id=?",Long.class,key)).isEqualTo(2);
    }

    @Test void concurrentRetriesAndEditsKeepOneStageAndOneWinner() throws Exception {
        String jwt=http.loginOwner(mockMvc),trip=trip(jwt).path("id").asText(),base="/trips/"+trip+"/stages";
        UUID key=UUID.randomUUID(); var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> create=()->mockMvc.perform(body(post(base).header("Idempotency-Key",key),INPUT).header("Authorization","Bearer "+jwt)).andReturn().getResponse().getStatus();
            var replies=pool.invokeAll(java.util.List.of(create,create));
            assertThat(replies.get(0).get()).isEqualTo(201);assertThat(replies.get(1).get()).isEqualTo(201);
            String changed=INPUT.replace("Quarto duplo","Suíte").replace("\"kind\"","\"expectedVersion\":1,\"kind\"");
            Callable<Integer> update=()->mockMvc.perform(body(patch(base+"/"+key),changed).header("Authorization","Bearer "+jwt)).andReturn().getResponse().getStatus();
            var edits=pool.invokeAll(java.util.List.of(update,update));
            assertThat(java.util.List.of(edits.get(0).get(),edits.get(1).get())).containsExactlyInAnyOrder(200,409);
            assertThat(jdbc.queryForObject("SELECT version FROM trip_stages WHERE id=?",Integer.class,key)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM trip_stage_events WHERE stage_id=? AND action='CREATED'",Long.class,key)).isEqualTo(1);
        } finally {pool.shutdownNow();}
    }

    @Test void deletingTripKeepsPersonalHistoryAndDoesNotFalselyFinishPrivacyRequest() throws Exception {
        String jwt=http.loginOwner(mockMvc);JsonNode trip=trip(jwt);String id=trip.path("id").asText(),person=trip.path("customerId").asText();
        String email="etapas-privacy-"+UUID.randomUUID()+"@test.local";
        call(jwt,body(patch("/customers/"+person),"{\"email\":\""+email+"\"}"),200);
        UUID key=UUID.randomUUID();call(jwt,body(post("/trips/"+id+"/stages").header("Idempotency-Key",key),INPUT),201);
        call(jwt,delete("/trips/"+id),204);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM trip_stages WHERE id=?",Long.class,key)).isZero();
        assertThat(jdbc.queryForObject("SELECT detail->>'customerId' FROM trip_stage_events WHERE stage_id=?",String.class,key)).isEqualTo(person);
        assertThat(call(jwt,delete("/customers/"+person),409).path("code").asText()).isEqualTo("TRIP_STAGE_HISTORY_EXISTS");
        String request=call(jwt,body(post("/public/data-deletion-request"),"{\"email\":\""+email+"\"}"),201).path("requestId").asText();
        assertThat(call(jwt,body(post("/agency/data-deletion-requests/"+request+"/process"),"{\"acao\":\"EXCLUIR\"}"),400).path("message").asText()).contains("continua pendente");
        assertThat(jdbc.queryForObject("SELECT status FROM data_deletion_requests WHERE id=?::uuid",String.class,request)).isEqualTo("PENDING");
        assertThat(call(jwt,get("/customers/"+person),200).path("email").asText()).isEqualTo(email);
    }

    @Test void agencyDeletionClearsStageHistoryAndLinksButPreservesOtherAgency() {
        Agency agency=agencies.save(Agency.builder().name("Descarte etapas").status(AgencyStatus.DELETION_PENDING)
                .subscriptionStatus(SubscriptionStatus.TRIAL).deletionScheduledAt(Instant.now().minusSeconds(20)).build());
        UUID a=agency.getId(),person=customers.save(CrmCustomer.builder().agency(agency).name("Pessoa").status(CustomerStatus.ACTIVE).notes("").build()).getId();
        accounts.save(PlatformAccount.builder().agency(agency).name("Equipe").email("f10-"+a+"@test.local").passwordHash("hash").accountKind(AccountKind.AGENCY_OWNER).active(true).build());
        UUID trip=UUID.randomUUID(),stage=UUID.randomUUID(),library=UUID.randomUUID();
        jdbc.update("INSERT INTO trips(id,agency_id,customer_id,service_type,status) VALUES(?,?,?,'AEREO','UPCOMING')",trip,a,person);
        jdbc.update("INSERT INTO trip_stages(id,agency_id,trip_id,kind,title,starts_at,created_by) VALUES(?,?,?,'HOTEL','Hotel',now(),'Teste')",stage,a,trip);
        jdbc.update("INSERT INTO agency_library(id,agency_id,title,category,kind,url) VALUES(?,?,'Guia','OUTROS','LINK','https://example.com')",library,a);
        jdbc.update("INSERT INTO trip_stage_links(id,stage_id,agency_id,library_id,author) VALUES(?,?,?,?,'Teste')",UUID.randomUUID(),stage,a,library);
        jdbc.update("INSERT INTO trip_stage_events(id,agency_id,trip_id,stage_id,action,detail,author) VALUES(?,?,?,?,'CREATED','{}','Teste')",UUID.randomUUID(),a,trip,stage);
        Agency retained=agencies.save(Agency.builder().name("Preservada etapas").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.TRIAL).build());
        UUID event=UUID.randomUUID();
        jdbc.update("INSERT INTO trip_stage_events(id,agency_id,trip_id,stage_id,action,detail,author) VALUES(?,?,?,?,'DELETED','{}','Teste')",event,retained.getId(),UUID.randomUUID(),UUID.randomUUID());
        scheduler.executeScheduledDeletions();
        assertThat(agencies.findById(a)).isEmpty();
        for(String table:java.util.List.of("trip_stage_events","trip_stage_links","trip_stages")) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM "+table+" WHERE agency_id=?",Long.class,a)).isZero();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM trip_stage_events WHERE id=?",Long.class,event)).isEqualTo(1);
    }
}
