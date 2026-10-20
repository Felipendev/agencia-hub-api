package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.*;
import com.agenciahub.api.application.persistence.repository.*;
import com.agenciahub.api.domain.*;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.scheduling.AgencyDeletionScheduler;
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

class AgendaLifecycleIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired CrmCustomerRepository customers;
    @Autowired AgencyRepository agencies;
    @Autowired PlatformAccountRepository accounts;
    @Autowired AgencyDeletionScheduler scheduler;
    private JsonNode call(String jwt, MockHttpServletRequestBuilder request, int code) throws Exception {
        String text=mockMvc.perform(request.header("Authorization","Bearer "+jwt)).andExpect(status().is(code)).andReturn().getResponse().getContentAsString();
        return text.isEmpty()?null:http.parse(text);
    }
    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request,String value) {
        return request.contentType(MediaType.APPLICATION_JSON).content(value);
    }
    private String person(String jwt) throws Exception {
        return call(jwt,body(post("/customers"),"{\"name\":\"Agenda revisão "+UUID.randomUUID()+"\",\"status\":\"ACTIVE\"}"),201).path("id").asText();
    }
    private String payload(String jwt,String person) throws Exception {
        String owner=call(jwt,get("/appointments/members"),200).get(0).path("id").asText();
        return "{\"customerId\":\""+person+"\",\"responsibleId\":\""+owner+"\",\"product\":\"MILES\",\"title\":\"Revisão\",\"startsAt\":\"2099-08-10T10:00:00-03:00\",\"durationMinutes\":60,\"timeZone\":\"America/Fortaleza\",\"format\":\"VIDEO\"}";
    }
    private String create(String jwt,String person) throws Exception {
        return call(jwt,body(post("/appointments").header("Idempotency-Key",UUID.randomUUID()),payload(jwt,person)),201).path("appointment").path("id").asText();
    }
    @Test void changedPayloadCannotReuseKeyAndStaleStatusCannotSucceed() throws Exception {
        String jwt=http.loginOwner(mockMvc),p=person(jwt),payload=payload(jwt,p); UUID key=UUID.randomUUID();
        call(jwt,body(post("/appointments").header("Idempotency-Key",key),payload),201);
        for(String changed:java.util.List.of(payload.replace(":60",":90"),payload.replace("VIDEO","PHONE"),payload.replace("\"format\"","\"purpose\":\"Outro objetivo\",\"format\""))) {
            call(jwt,body(post("/appointments").header("Idempotency-Key",key),changed),409);
        }
        call(jwt,body(post("/appointments/"+key+"/status"),"{\"status\":\"CANCELLED\",\"reason\":\"Pedido do cliente\",\"expectedVersion\":1}"),200);
        call(jwt,body(post("/appointments/"+key+"/status"),"{\"status\":\"CANCELLED\",\"expectedVersion\":1}"),409);
    }
    @Test void deletedAppointmentStillBlocksSilentCustomerDeletionAndAnonymization() throws Exception {
        String jwt=http.loginOwner(mockMvc),p=person(jwt),email="agenda-delete-"+UUID.randomUUID()+"@test.local",id=create(jwt,p);
        call(jwt,body(patch("/customers/"+p),"{\"email\":\""+email+"\"}"),200);
        assertThat(call(jwt,delete("/customers/"+p),409).path("code").asText()).isEqualTo("AGENDA_HISTORY_EXISTS");
        call(jwt,delete("/appointments/"+id).param("expectedVersion","1"),204);
        assertThat(call(jwt,delete("/customers/"+p),409).path("code").asText()).isEqualTo("AGENDA_HISTORY_EXISTS");
        String req=call(jwt,body(post("/public/data-deletion-request"),"{\"email\":\""+email+"\"}"),201).path("requestId").asText();
        assertThat(call(jwt,body(post("/agency/data-deletion-requests/"+req+"/process"),"{\"acao\":\"EXCLUIR\"}"),400).path("message").asText()).contains("continua pendente");
        assertThat(jdbc.queryForObject("SELECT status FROM data_deletion_requests WHERE id=?::uuid",String.class,req)).isEqualTo("PENDING");
        assertThat(call(jwt,get("/customers/"+p),200).path("email").asText()).isEqualTo(email);
    }
    @Test void concurrentCreationAndEditionKeepSingleRecordAndVersion() throws Exception {
        String jwt=http.loginOwner(mockMvc),p=person(jwt),payload=payload(jwt,p); UUID key=UUID.randomUUID();
        var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> create=()->mockMvc.perform(body(post("/appointments").header("Idempotency-Key",key),payload).header("Authorization","Bearer "+jwt)).andReturn().getResponse().getStatus();
            var replies=pool.invokeAll(java.util.List.of(create,create));
            assertThat(java.util.List.of(replies.get(0).get(),replies.get(1).get())).containsExactly(201,201);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM appointments WHERE id=?",Long.class,key)).isEqualTo(1L);
            Callable<Integer> edit=()->mockMvc.perform(body(patch("/appointments/"+key),payload.replace("Revisão","Reagendado").replace("\"format\"","\"expectedVersion\":1,\"format\"")).header("Authorization","Bearer "+jwt)).andReturn().getResponse().getStatus();
            replies=pool.invokeAll(java.util.List.of(edit,edit));
            assertThat(java.util.List.of(replies.get(0).get(),replies.get(1).get())).containsExactlyInAnyOrder(200,409);
        } finally {pool.shutdownNow();}
    }
    @Test void agencyRemovalCleansMinutesVersionsAndDeletedEventsWithoutAffectingOtherAgency() throws Exception {
        String jwt=http.loginOwner(mockMvc),p=person(jwt),retained=create(jwt,p);
        Agency agency=agencies.save(Agency.builder().name("Descartar agenda").status(AgencyStatus.DELETION_PENDING).subscriptionStatus(SubscriptionStatus.ACTIVE).deletionScheduledAt(Instant.now().minusSeconds(60)).build());
        PlatformAccount owner=accounts.save(PlatformAccount.builder().agency(agency).name("Responsável").email(UUID.randomUUID()+"@test.local").passwordHash("test").accountKind(AccountKind.AGENCY_OWNER).active(true).build());
        CrmCustomer customer=customers.save(CrmCustomer.builder().agency(agency).name("Pessoa").status(CustomerStatus.ACTIVE).build());
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO appointments(id,agency_id,customer_id,product,title,starts_at,duration_minutes,format,responsible_id,created_by) VALUES(?,?,?,'MILES','Teste',now(),60,'VIDEO',?,'teste')",id,agency.getId(),customer.getId(),owner.getId());
        jdbc.update("INSERT INTO appointment_minutes(appointment_id,agency_id,data,search_text) VALUES(?,?,?::jsonb,'Texto pessoal')",id,agency.getId(),"{\"summary\":\"Texto pessoal\"}");
        jdbc.update("INSERT INTO appointment_minutes_versions(id,appointment_id,agency_id,version,data,author) VALUES(?,?,?,1,'{}'::jsonb,'autor')",UUID.randomUUID(),id,agency.getId());
        jdbc.update("INSERT INTO appointment_events(id,agency_id,appointment_id,customer_id,action,detail,author) VALUES(?,?,?,?,'DELETED','{}'::jsonb,'autor')",UUID.randomUUID(),agency.getId(),UUID.randomUUID(),customer.getId());
        scheduler.executeScheduledDeletions();
        for(String table:java.util.List.of("appointments","appointment_minutes","appointment_minutes_versions","appointment_events")) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM "+table+" WHERE agency_id=?",Long.class,agency.getId())).isZero();
        }
        call(jwt,get("/appointments/"+retained),200);
    }
}
