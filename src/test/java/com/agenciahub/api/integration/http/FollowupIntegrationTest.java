package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.*;
import com.agenciahub.api.application.persistence.repository.*;
import com.agenciahub.api.domain.*;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.scheduling.AgencyDeletionScheduler;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FollowupIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AgencyRepository agencies;
    @Autowired PlatformAccountRepository accounts;
    @Autowired CrmCustomerRepository customers;
    @Autowired PasswordEncoder passwords;
    @Autowired AgencyDeletionScheduler scheduler;
    private static final LocalDate TODAY=LocalDate.now(ZoneId.of("America/Fortaleza"));
    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request,String value){return request.contentType(MediaType.APPLICATION_JSON).content(value);}
    private JsonNode call(String jwt,MockHttpServletRequestBuilder request,int expected)throws Exception {
        String response=mockMvc.perform(request.header("Authorization","Bearer "+jwt)).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return response.isEmpty()?null:http.parse(response);
    }
    private String person(String jwt)throws Exception {
        return call(jwt,body(post("/customers"),"{\"name\":\"Contato F08 "+UUID.randomUUID()+"\",\"email\":\"f08-"+UUID.randomUUID()+"@test.local\",\"status\":\"PROSPECT\"}"),201).path("id").asText();
    }
    private UUID owner(){return jdbc.queryForObject("SELECT id FROM users WHERE email='admin@agenciahub.com'",UUID.class);}
    private String input(String person,String product,LocalDate due,int version){return JsonNodeFactory.instance.objectNode()
            .put("customerId",person).put("product",product).put("title","Retornar com proposta").put("notes","Contato prefere mensagens")
            .put("dueOn",due.toString()).put("responsibleId",owner().toString()).put("expectedVersion",version).toString();}
    private JsonNode create(String jwt,String input,UUID key)throws Exception{return call(jwt,body(post("/follow-ups").header("Idempotency-Key",key),input),201);}
    private JsonNode list(String jwt,String person,String product,String bucket)throws Exception{return call(jwt,get("/follow-ups").param("product",product).param("customerId",person).param("bucket",bucket),200);}
    private String state(String state,int version,String reason){return "{\"state\":\""+state+"\",\"expectedVersion\":"+version+",\"reason\":\""+reason+"\"}";}

    @Test void crudSameIdHistoryAndNoBusinessSideEffects()throws Exception {
        String jwt=http.loginOwner(mockMvc),person=person(jwt),payload=input(person,"TRAVEL",TODAY,0);UUID key=UUID.randomUUID();
        Map<String,Long> before=new LinkedHashMap<>(); for(String table:List.of("sales","financial_entries","work_tasks","appointments","trips")) before.put(table,jdbc.queryForObject("SELECT count(*) FROM "+table,Long.class));
        JsonNode created=create(jwt,payload,key);String id=created.path("id").asText();
        assertThat(create(jwt,payload,key).path("id").asText()).isEqualTo(id);
        call(jwt,body(post("/follow-ups").header("Idempotency-Key",key),payload.replace("Contato prefere mensagens","Outro texto")),409);
        String changed=input(person,"TRAVEL",TODAY.plusDays(3),1).replace("Retornar com proposta","Revisar proposta");
        JsonNode edited=call(jwt,body(patch("/follow-ups/"+id),changed),200);
        assertThat(edited.path("id").asText()).isEqualTo(id);assertThat(edited.path("version").asInt()).isEqualTo(2);
        assertThat(call(jwt,get("/follow-ups/"+id),200).path("dueOn").asText()).isEqualTo(TODAY.plusDays(3).toString());
        call(jwt,body(patch("/follow-ups/"+id),changed),409);
        JsonNode events=call(jwt,get("/follow-ups/"+id+"/history"),200);
        assertThat(events.size()).isEqualTo(2);assertThat(events.get(1).path("action").asText()).isEqualTo("RESCHEDULED");
        assertThat(events.get(1).path("snapshot").path("before").path("dueOn").asText()).isEqualTo(TODAY.toString());
        call(jwt,delete("/follow-ups/"+id).param("expectedVersion","1"),409);
        call(jwt,delete("/follow-ups/"+id).param("expectedVersion","2"),204);
        call(jwt,get("/follow-ups/"+id),404);
        assertThat(list(jwt,person,"TRAVEL","ALL").path("items").size()).isZero();
        assertThat(call(jwt,get("/follow-ups").param("product","TRAVEL").param("customerId",person).param("deleted","true"),200).path("items").get(0).path("deleted").asBoolean()).isTrue();
        assertThat(call(jwt,get("/follow-ups/"+id+"/history"),200).size()).isEqualTo(3);
        call(jwt,body(post("/follow-ups").header("Idempotency-Key",key),payload),409);
        before.forEach((table,count)->assertThat(jdbc.queryForObject("SELECT count(*) FROM "+table,Long.class)).as(table).isEqualTo(count));
    }
    @Test void bucketsUseFortalezaDateAndKeepProductsIndependent()throws Exception {
        String jwt=http.loginOwner(mockMvc),person=person(jwt);
        create(jwt,input(person,"TRAVEL",TODAY.minusDays(1),0),UUID.randomUUID());
        String todayId=create(jwt,input(person,"TRAVEL",TODAY,0),UUID.randomUUID()).path("id").asText();
        create(jwt,input(person,"TRAVEL",TODAY.plusYears(2),0),UUID.randomUUID());
        String miles=create(jwt,input(person,"MILES",TODAY,0),UUID.randomUUID()).path("id").asText();
        JsonNode board=list(jwt,person,"TRAVEL","ALL");assertThat(board.path("today").asText()).isEqualTo(TODAY.toString());
        assertThat(board.path("counts").path("overdue").asLong()).isEqualTo(1);assertThat(board.path("counts").path("today").asLong()).isEqualTo(1);assertThat(board.path("counts").path("upcoming").asLong()).isEqualTo(1);
        for(String bucket:List.of("OVERDUE","TODAY","UPCOMING")) assertThat(list(jwt,person,"TRAVEL",bucket).path("items").size()).isEqualTo(1);
        call(jwt,body(post("/follow-ups/"+todayId+"/state"),state("DONE",1,"Conversamos")),200);
        assertThat(list(jwt,person,"TRAVEL","ALL").path("counts").path("today").asInt()).isZero();
        assertThat(call(jwt,get("/follow-ups/"+miles),200).path("state").asText()).isEqualTo("OPEN");
        assertThat(list(jwt,person,"MILES","ALL").path("items").size()).isEqualTo(1);
        assertThat(call(jwt,get("/follow-ups").param("product","MILES").param("customerId",person).param("responsibleId",UUID.randomUUID().toString()),200).path("items").size()).isZero();
    }
    @Test void transitionsRequireReasonVersionAndReopeningBeforeEditing()throws Exception {
        String jwt=http.loginOwner(mockMvc),person=person(jwt),id=create(jwt,input(person,"MILES",TODAY,0),UUID.randomUUID()).path("id").asText();
        call(jwt,body(post("/follow-ups/"+id+"/state"),state("CANCELLED",1,"")),400);
        call(jwt,body(post("/follow-ups/"+id+"/state"),state("CANCELLED",1,"Sem interesse agora")),200);
        call(jwt,body(post("/follow-ups/"+id+"/state"),state("CANCELLED",1,"Reenvio antigo")),409);
        call(jwt,body(post("/follow-ups/"+id+"/state"),state("CANCELLED",2,"Mesmo estado")),200);
        call(jwt,body(patch("/follow-ups/"+id),input(person,"MILES",TODAY.plusDays(1),2)),409);
        call(jwt,delete("/follow-ups/"+id).param("expectedVersion","2"),409);
        call(jwt,body(post("/follow-ups/"+id+"/state"),state("DONE",2,"")),409);
        call(jwt,body(post("/follow-ups/"+id+"/state"),state("OPEN",2,"")),400);
        call(jwt,body(post("/follow-ups/"+id+"/state"),state("OPEN",2,"Retomou contato")),200);
        call(jwt,body(patch("/follow-ups/"+id),input(person,"MILES",TODAY.plusDays(1),3)),200);
        call(jwt,body(patch("/follow-ups/"+id),input(person,"TRAVEL",TODAY,4)),400);
        call(jwt,body(patch("/follow-ups/"+id),input(person(jwt),"MILES",TODAY,4)),400);
        call(jwt,body(post("/follow-ups/"+id+"/state"),state("DONE",4,"Contato realizado")),200);
    }
    @Test void validationAndMissingRecordsDoNotPretendSuccess()throws Exception {
        String jwt=http.loginOwner(mockMvc),person=person(jwt),valid=input(person,"TRAVEL",TODAY,0);
        for(String invalid:List.of(valid.replace("Retornar com proposta"," "),valid.replace("\"responsibleId\":\""+owner()+"\"","\"responsibleId\":null"),valid.replace(person,UUID.randomUUID().toString()),valid.replace(owner().toString(),UUID.randomUUID().toString()),valid.replace("\"expectedVersion\":0","\"expectedVersion\":1"))) call(jwt,body(post("/follow-ups").header("Idempotency-Key",UUID.randomUUID()),invalid),400);
        call(jwt,get("/follow-ups"),400);call(jwt,get("/follow-ups").param("product","INVALID"),400);
        String id=UUID.randomUUID().toString();call(jwt,get("/follow-ups/"+id),404);call(jwt,get("/follow-ups/"+id+"/history"),404);
        call(jwt,body(patch("/follow-ups/"+id),input(person,"TRAVEL",TODAY,1)),404);
        call(jwt,delete("/follow-ups/"+id).param("expectedVersion","1"),404);
    }
    private String member(AccountKind kind,Agency agency)throws Exception {
        String email="f08-"+UUID.randomUUID()+"@test.local";
        accounts.save(PlatformAccount.builder().agency(agency).name("Equipe F08").email(email).passwordHash(passwords.encode("followup-test-password")).accountKind(kind).active(true).emailVerified(true).termsAccepted(true).build());
        return call("unused",body(post("/auth/login"),"{\"email\":\""+email+"\",\"password\":\"followup-test-password\"}"),200).path("token").asText();
    }
    @Test void rolesAndTenantIsolationCoverAllOperations()throws Exception {
        String jwt=http.loginOwner(mockMvc),person=person(jwt),id=create(jwt,input(person,"TRAVEL",TODAY,0),UUID.randomUUID()).path("id").asText();
        Agency other=agencies.save(Agency.builder().name("Outra F08").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.TRIAL).build());
        String foreign=member(AccountKind.AGENCY_OWNER,other);
        for(var request:List.of(get("/follow-ups/"+id),get("/follow-ups/"+id+"/history"),delete("/follow-ups/"+id).param("expectedVersion","1"),body(patch("/follow-ups/"+id),input(person,"TRAVEL",TODAY,1)),body(post("/follow-ups/"+id+"/state"),state("DONE",1,"")))) call(foreign,request,404);
        assertThat(list(foreign,person,"TRAVEL","ALL").path("items").size()).isZero();
        call(foreign,body(post("/follow-ups").header("Idempotency-Key",UUID.randomUUID()),input(person,"TRAVEL",TODAY,0)),400);
        Agency own=agencies.findById(jdbc.queryForObject("SELECT agency_id FROM users WHERE id=?",UUID.class,owner())).orElseThrow();
        String seller=member(AccountKind.SALES_AGENT,own);call(seller,get("/follow-ups/"+id),200);
        call(seller,body(patch("/follow-ups/"+id),input(person,"TRAVEL",TODAY.plusDays(2),1)),200);
        String admin=member(AccountKind.PLATFORM_ADMIN,null);call(admin,get("/follow-ups").param("product","TRAVEL"),403);
        mockMvc.perform(get("/follow-ups").param("product","TRAVEL")).andExpect(status().isForbidden());
    }
    @Test void concurrentRequestsHaveOneCreationAndOneSuccessfulEdition()throws Exception {
        String jwt=http.loginOwner(mockMvc),person=person(jwt),payload=input(person,"TRAVEL",TODAY,0);UUID key=UUID.randomUUID();var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<String> create=()->create(jwt,payload,key).path("id").asText();var created=pool.invokeAll(List.of(create,create));String id=created.get(0).get();assertThat(created.get(1).get()).isEqualTo(id);
            Callable<Integer> update=()->mockMvc.perform(body(patch("/follow-ups/"+id),input(person,"TRAVEL",TODAY.plusDays(2),1)).header("Authorization","Bearer "+jwt)).andReturn().getResponse().getStatus();
            var changed=pool.invokeAll(List.of(update,update));assertThat(List.of(changed.get(0).get(),changed.get(1).get())).containsExactlyInAnyOrder(200,409);
            assertThat(call(jwt,get("/follow-ups/"+id+"/history"),200).size()).isEqualTo(2);
        } finally { pool.shutdownNow(); }
    }
    @Test void deletedActionsKeepPrivacyReviewPending()throws Exception {
        String jwt=http.loginOwner(mockMvc),person=person(jwt),id=create(jwt,input(person,"TRAVEL",TODAY,0),UUID.randomUUID()).path("id").asText();
        String email=jdbc.queryForObject("SELECT email FROM customers WHERE id=?::uuid",String.class,person);
        call(jwt,delete("/follow-ups/"+id).param("expectedVersion","1"),204);
        assertThat(call(jwt,delete("/customers/"+person),409).path("code").asText()).isEqualTo("FOLLOWUP_HISTORY_EXISTS");
        String request=call(jwt,body(post("/public/data-deletion-request"),"{\"email\":\""+email+"\"}"),201).path("requestId").asText();
        assertThat(call(jwt,body(post("/agency/data-deletion-requests/"+request+"/process"),"{\"acao\":\"EXCLUIR\"}"),400).path("message").asText()).contains("continua pendente");
        assertThat(jdbc.queryForObject("SELECT status FROM data_deletion_requests WHERE id=?::uuid",String.class,request)).isEqualTo("PENDING");
    }
    @Test void scheduledAgencyDeletionClearsFollowupsAndPreservesOtherAgency()throws Exception {
        String jwt=http.loginOwner(mockMvc),person=person(jwt),id=create(jwt,input(person,"MILES",TODAY,0),UUID.randomUUID()).path("id").asText();
        Agency doomed=agencies.save(Agency.builder().name("Descartar F08").status(AgencyStatus.DELETION_PENDING).subscriptionStatus(SubscriptionStatus.TRIAL).deletionScheduledAt(Instant.now().minusSeconds(10)).build());
        UUID customer=customers.save(CrmCustomer.builder().agency(doomed).name("Cliente F08").status(CustomerStatus.ACTIVE).notes("").build()).getId(),followup=UUID.randomUUID();
        UUID user=accounts.save(PlatformAccount.builder().agency(doomed).name("Equipe").email("f08-delete-"+UUID.randomUUID()+"@test.local").passwordHash("hash").accountKind(AccountKind.AGENCY_OWNER).active(true).build()).getId();
        jdbc.update("INSERT INTO commercial_followups(id,agency_id,customer_id,product,title,due_on,responsible_id,creation_key,creation_payload) VALUES(?,?,?,'TRAVEL','Contato',current_date,?,?,'{}')",followup,doomed.getId(),customer,user,UUID.randomUUID());
        jdbc.update("INSERT INTO commercial_followup_events(id,agency_id,followup_id,customer_id,action,snapshot,author_id) VALUES(?,?,?,?,'CREATED','{}',?)",UUID.randomUUID(),doomed.getId(),followup,customer,user);
        scheduler.executeScheduledDeletions();assertThat(agencies.findById(doomed.getId())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM commercial_followups WHERE agency_id=?",Long.class,doomed.getId())).isZero();
        assertThat(call(jwt,get("/follow-ups/"+id+"/history"),200).size()).isEqualTo(1);
    }
}
