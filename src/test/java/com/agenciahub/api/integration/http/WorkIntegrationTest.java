package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.*;
import com.agenciahub.api.application.persistence.repository.*;
import com.agenciahub.api.domain.*;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AgencyRepository agencies;
    @Autowired CrmCustomerRepository customers;
    @Autowired PlatformAccountRepository accounts;
    @Autowired PasswordEncoder passwords;
    @Autowired com.agenciahub.api.scheduling.AgencyDeletionScheduler deletionScheduler;
    private JsonNode call(String jwt,MockHttpServletRequestBuilder request,int expected) throws Exception {
        String response=mockMvc.perform(request.header("Authorization","Bearer "+jwt)).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return response.isEmpty()?null:http.parse(response);
    }
    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request,String payload){return request.contentType(MediaType.APPLICATION_JSON).content(payload);}
    private String person(String jwt)throws Exception{return call(jwt,body(post("/customers"),"{\"name\":\"F01 "+UUID.randomUUID()+"\",\"status\":\"ACTIVE\"}"),201).path("id").asText();}
    private String task(String person,String title,int version){return "{\"product\":\"MILES\",\"customerId\":\"%s\",\"title\":\"%s\",\"notes\":\"revisar plano\",\"dueOn\":\"2026-10-23\",\"expectedVersion\":%d}".formatted(person,title,version);}
    private String template(String name,String frequency,int version){return "{\"product\":\"MILES\",\"name\":\"%s\",\"frequency\":\"%s\",\"steps\":[{\"title\":\"Revisar saldo\",\"dueOffsetDays\":0},{\"title\":\"Verificar validade\",\"dueOffsetDays\":2}],\"expectedVersion\":%d}".formatted(name,frequency,version);}
    private JsonNode create(String jwt,String payload,UUID key)throws Exception{return call(jwt,body(post("/work/tasks"),payload).header("Idempotency-Key",key),201);}
    private String member(Agency agency,AccountKind kind)throws Exception{
        String email="work-"+UUID.randomUUID()+"@test.local";
        accounts.save(PlatformAccount.builder().agency(agency).name("Equipe F01").email(email).passwordHash(passwords.encode("test1234")).active(true).emailVerified(true).accountKind(kind).mustChangePassword(false).build());
        return call("",body(post("/auth/login"),"{\"email\":\""+email+"\",\"password\":\"test1234\"}"),200).path("token").asText();
    }
    @Test void taskCrudPersistsSameIdWithVersionAndAudit()throws Exception{
        String jwt=http.loginOwner(mockMvc),person=person(jwt);UUID key=UUID.randomUUID();String payload=task(person,"Conferir clube",0);
        JsonNode saved=create(jwt,payload,key);String id=saved.path("id").asText();
        assertThat(create(jwt,payload,key).path("id").asText()).isEqualTo(id);
        call(jwt,body(post("/work/tasks"),payload.replace("Conferir clube","Outra tarefa")).header("Idempotency-Key",key),409);
        JsonNode updated=call(jwt,body(patch("/work/tasks/"+id),task(person,"Clube revisado",1)),200);
        assertThat(updated.path("id").asText()).isEqualTo(id);assertThat(updated.path("version").asInt()).isEqualTo(2);
        assertThat(call(jwt,get("/work/tasks/"+id),200).path("title").asText()).isEqualTo("Clube revisado");
        assertThat(call(jwt,get("/work/tasks").param("customerId",person).param("product","TRAVEL"),200)).isEmpty();
        assertThat(call(jwt,get("/work/tasks").param("customerId",person).param("product","MILES").param("dueBefore","2026-10-24"),200).size()).isEqualTo(1);
        call(jwt,body(patch("/work/tasks/"+id),task(person,"Conflito",1)),409);
        call(jwt,delete("/work/tasks/"+id).param("expectedVersion","1"),409);
        call(jwt,delete("/work/tasks/"+id).param("expectedVersion","2"),204);
        call(jwt,get("/work/tasks/"+id),404);
        JsonNode events=call(jwt,get("/work/history/TASK/"+id),200);
        assertThat(events.size()).isEqualTo(3);assertThat(events.get(0).path("snapshot").path("title").asText()).isEqualTo("Conferir clube");
        assertThat(events.get(2).path("action").asText()).isEqualTo("DELETED");assertThat(events.get(2).path("authorId").asText()).isNotBlank();
        call(jwt,body(post("/work/tasks"),payload).header("Idempotency-Key",key),409);
    }
    @Test void templatesAndWeeklyRunsKeepSnapshotsAndDoNotDuplicate()throws Exception{
        String jwt=http.loginOwner(mockMvc),person=person(jwt);
        JsonNode model=call(jwt,body(post("/work/templates"),template("Rotina semanal","WEEKLY",0)),201);String id=model.path("id").asText();
        String input="{\"customerId\":\""+person+"\",\"date\":\"2026-10-21\"}";
        JsonNode run=call(jwt,body(post("/work/templates/"+id+"/apply"),input),200);String runId=run.path("id").asText();
        assertThat(run.path("periodStart").asText()).isEqualTo("2026-10-19");assertThat(run.path("periodEnd").asText()).isEqualTo("2026-10-25");
        call(jwt,body(patch("/work/templates/"+id),template("Modelo novo","WEEKLY",1)),200);
        assertThat(call(jwt,body(post("/work/templates/"+id+"/apply"),input.replace("2026-10-21","2026-10-25")),200).path("id").asText()).isEqualTo(runId);
        assertThat(call(jwt,get("/work/runs/"+runId),200).path("snapshot").path("name").asText()).isEqualTo("Rotina semanal");
        JsonNode tasks=call(jwt,get("/work/tasks").param("runId",runId),200);assertThat(tasks.size()).isEqualTo(2);
        assertThat(tasks.get(1).path("dueOn").asText()).isEqualTo("2026-10-21");
        call(jwt,body(patch("/work/runs/"+runId),"{\"title\":\"Revisão da semana\",\"notes\":\"Conferido\",\"expectedVersion\":1}"),200);
        assertThat(call(jwt,get("/work/runs/"+runId),200).path("title").asText()).isEqualTo("Revisão da semana");
        JsonNode next=call(jwt,body(post("/work/templates/"+id+"/apply"),input.replace("2026-10-21","2026-10-26")),200);
        assertThat(next.path("id").asText()).isNotEqualTo(runId);assertThat(next.path("snapshot").path("name").asText()).isEqualTo("Modelo novo");
        call(jwt,delete("/work/templates/"+id).param("expectedVersion","2"),204);
        call(jwt,body(post("/work/templates/"+id+"/apply"),input),404);
        assertThat(call(jwt,get("/work/runs/"+runId),200).path("snapshot").path("steps").size()).isEqualTo(2);
    }
    @Test void runCancellationPreservesCompletedWorkAndRequiresExplicitReopening()throws Exception{
        String jwt=http.loginOwner(mockMvc),person=person(jwt);
        String model=call(jwt,body(post("/work/templates"),template("Inicial","ONCE",0)),201).path("id").asText();
        String run=call(jwt,body(post("/work/templates/"+model+"/apply"),"{\"customerId\":\""+person+"\",\"date\":\"2026-10-20\"}"),200).path("id").asText();
        JsonNode rows=call(jwt,get("/work/tasks").param("runId",run),200);String done=rows.get(0).path("id").asText(),open=rows.get(1).path("id").asText();
        JsonNode closed=call(jwt,body(post("/work/tasks/"+done+"/state"),"{\"state\":\"DONE\",\"expectedVersion\":1}"),200);
        call(jwt,body(patch("/work/tasks/"+done),task(person,"Não pode",2)),409);
        call(jwt,delete("/work/tasks/"+done).param("expectedVersion","2"),409);
        call(jwt,body(post("/work/runs/"+run+"/cancel"),"{\"reason\":\"Cliente suspendeu o plano\",\"expectedVersion\":1}"),200);
        JsonNode preserved=call(jwt,get("/work/tasks/"+done),200);assertThat(preserved.path("stateAt")).isEqualTo(closed.path("stateAt"));
        assertThat(call(jwt,get("/work/runs/"+run),200).path("cancelReason").asText()).isEqualTo("Cliente suspendeu o plano");
        assertThat(preserved.path("state").asText()).isEqualTo("DONE");assertThat(call(jwt,get("/work/tasks/"+open),200).path("state").asText()).isEqualTo("CANCELLED");
        call(jwt,body(post("/work/tasks/"+open+"/state"),"{\"state\":\"OPEN\",\"expectedVersion\":2}"),409);
        call(jwt,body(post("/work/runs/"+run+"/cancel"),"{\"reason\":\"Antigo\",\"expectedVersion\":1}"),409);
    }
    @Test void monthlyAndOncePeriodsAndTaskReopeningAreExplicit()throws Exception{
        String jwt=http.loginOwner(mockMvc),person=person(jwt);
        for(String frequency:List.of("MONTHLY","ONCE")){
            String model=call(jwt,body(post("/work/templates"),template("Frequência "+frequency,frequency,0)),201).path("id").asText();
            String input="{\"customerId\":\""+person+"\",\"date\":\"2026-02-17\"}";
            JsonNode first=call(jwt,body(post("/work/templates/"+model+"/apply"),input),200);
            JsonNode repeat=call(jwt,body(post("/work/templates/"+model+"/apply"),input.replace("2026-02-17","2026-02-28")),200);
            assertThat(repeat.path("id")).isEqualTo(first.path("id"));
            if(frequency.equals("MONTHLY")){assertThat(first.path("periodStart").asText()).isEqualTo("2026-02-01");assertThat(first.path("periodEnd").asText()).isEqualTo("2026-02-28");}
        }
        String id=create(jwt,task(person,"Avulsa",0),UUID.randomUUID()).path("id").asText();
        call(jwt,body(post("/work/tasks/"+id+"/state"),"{\"state\":\"CANCELLED\",\"reason\":\"\",\"expectedVersion\":1}"),400);
        call(jwt,body(post("/work/tasks/"+id+"/state"),"{\"state\":\"CANCELLED\",\"reason\":\"Pausa\",\"expectedVersion\":1}"),200);
        call(jwt,body(post("/work/tasks/"+id+"/state"),"{\"state\":\"DONE\",\"expectedVersion\":2}"),409);
        call(jwt,body(post("/work/tasks/"+id+"/state"),"{\"state\":\"OPEN\",\"expectedVersion\":2}"),200);
        call(jwt,body(patch("/work/tasks/"+id),task(person,"Editada",3)),200);
    }
    @Test void tenantRolesAndForeignReferencesAreProtected()throws Exception{
        String jwt=http.loginOwner(mockMvc),person=person(jwt);
        Agency own=customers.findById(UUID.fromString(person)).orElseThrow().getAgency();
        String seller=member(own,AccountKind.SALES_AGENT);String id=create(jwt,task(person,"Compartilhada",0),UUID.randomUUID()).path("id").asText();
        assertThat(call(seller,get("/work/tasks/"+id),200).path("title").asText()).isEqualTo("Compartilhada");
        call(member(own,AccountKind.PLATFORM_ADMIN),get("/work/tasks"),403);mockMvc.perform(get("/work/tasks")).andExpect(IntegrationHttpSupport.unauthenticated());
        Agency foreign=agencies.save(Agency.builder().name("Outra F01").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.ACTIVE).build());
        String other=member(foreign,AccountKind.AGENCY_OWNER),otherPerson=person(other);
        call(other,get("/work/tasks/"+id),404);call(other,body(patch("/work/tasks/"+id),task(otherPerson,"Invasão",1)),404);
        call(other,delete("/work/tasks/"+id).param("expectedVersion","1"),404);call(other,get("/work/history/TASK/"+id),404);
        call(other,body(post("/work/tasks/"+id+"/state"),"{\"state\":\"DONE\",\"expectedVersion\":1}"),404);
        call(jwt,body(post("/work/tasks"),task(otherPerson,"Vínculo inválido",0)).header("Idempotency-Key",UUID.randomUUID()),400);
        String foreignMember=call(other,get("/work/members"),200).get(0).path("id").asText();
        String invalid=task(person,"Responsável inválido",0).replace("\"notes\"","\"responsibleId\":\""+foreignMember+"\",\"notes\"");
        call(jwt,body(post("/work/tasks"),invalid).header("Idempotency-Key",UUID.randomUUID()),400);
        String model=call(jwt,body(post("/work/templates"),template("Privado","ONCE",0)),201).path("id").asText();
        call(other,body(patch("/work/templates/"+model),template("Invasão","ONCE",1)),404);
        call(other,delete("/work/templates/"+model).param("expectedVersion","1"),404);
        call(other,body(post("/work/templates/"+model+"/apply"),"{\"customerId\":\""+otherPerson+"\",\"date\":\"2026-10-20\"}"),404);
        assertThat(call(other,get("/work/tasks"),200)).isEmpty();
    }
    @Test void concurrentCreatesAndAppliesAreIdempotent()throws Exception{
        String jwt=http.loginOwner(mockMvc),person=person(jwt);UUID key=UUID.randomUUID();String payload=task(person,"Concorrente",0);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try{
            List<Future<String>> created=new ArrayList<>();
            for(int i=0;i<2;i++)created.add(pool.submit(()->create(jwt,payload,key).path("id").asText()));
            assertThat(created.get(0).get(30,TimeUnit.SECONDS)).isEqualTo(created.get(1).get(30,TimeUnit.SECONDS));
            String model=call(jwt,body(post("/work/templates"),template("Concorrência","WEEKLY",0)),201).path("id").asText();
            Callable<String> apply=()->call(jwt,body(post("/work/templates/"+model+"/apply"),"{\"customerId\":\""+person+"\",\"date\":\"2026-10-20\"}"),200).path("id").asText();
            Future<String> first=pool.submit(apply),second=pool.submit(apply);
            String run=first.get(30,TimeUnit.SECONDS);assertThat(second.get(30,TimeUnit.SECONDS)).isEqualTo(run);
            assertThat(call(jwt,get("/work/tasks").param("runId",run),200).size()).isEqualTo(2);
        } finally { pool.shutdownNow(); }
    }
    @Test void invalidInputsAndCrossProductLinksAreRejected()throws Exception{
        String jwt=http.loginOwner(mockMvc),person=person(jwt);
        call(jwt,body(post("/work/tasks"),task(person,"",0)).header("Idempotency-Key",UUID.randomUUID()),400);
        String invalid=task(person,"Vínculo",0).replace("\"notes\"","\"tripId\":\""+UUID.randomUUID()+"\",\"notes\"");
        call(jwt,body(post("/work/tasks"),invalid).header("Idempotency-Key",UUID.randomUUID()),400);
        call(jwt,body(post("/work/templates"),template("Inválido","WEEKLY",0).replace("\"dueOffsetDays\":2","\"dueOffsetDays\":-1")),400);
        call(jwt,body(post("/work/templates"),template("Inválido","WEEKLY",0).replace("\"steps\":[","\"steps\":[null,")),400);
        assertThat(call(jwt,get("/work/tasks").param("customerId",person),200)).isEmpty();
    }
    @Test void privacyRequestStaysPendingWhenDeletedTasksRetainPersonalHistory()throws Exception{
        String jwt=http.loginOwner(mockMvc),person=person(jwt),email="work-privacy-"+UUID.randomUUID()+"@test.local";
        call(jwt,body(patch("/customers/"+person),"{\"email\":\""+email+"\"}"),200);
        String task=create(jwt,task(person,"Plano com histórico",0),UUID.randomUUID()).path("id").asText();
        call(jwt,delete("/work/tasks/"+task).param("expectedVersion","1"),204);
        assertThat(call(jwt,delete("/customers/"+person),409).path("code").asText()).isEqualTo("WORK_HISTORY_EXISTS");
        String request=call(jwt,body(post("/public/data-deletion-request"),"{\"email\":\""+email+"\"}"),201).path("requestId").asText();
        JsonNode blocked=call(jwt,body(post("/agency/data-deletion-requests/"+request+"/process"),"{\"acao\":\"EXCLUIR\"}"),400);
        assertThat(blocked.path("message").asText()).contains("continua pendente");
        assertThat(jdbc.queryForObject("SELECT status FROM data_deletion_requests WHERE id=?::uuid",String.class,request)).isEqualTo("PENDING");
        assertThat(call(jwt,get("/customers/"+person),200).path("email").asText()).isEqualTo(email);
    }
    @Test void travelLinksMustBelongToTheSamePersonAndAgency()throws Exception{
        String jwt=http.loginOwner(mockMvc),person=person(jwt),another=person(jwt);
        UUID agency=customers.findById(UUID.fromString(person)).orElseThrow().getAgency().getId(),trip=UUID.randomUUID();
        jdbc.update("INSERT INTO trips(id,agency_id,customer_id,service_type,status,booking_locator) VALUES(?,?,?::uuid,'FLIGHT','UPCOMING','F01REF')",trip,agency,person);
        String quote=call(jwt,body(post("/quotations"),"{\"customerId\":\""+person+"\",\"title\":\"Viagem F01\",\"destination\":\"Lisboa\",\"totalAmount\":1000,\"validUntil\":\"2027-12-31\"}"),201).path("id").asText();
        String payload=task(person,"Preparar viagem",0).replace("MILES","TRAVEL").replace("\"notes\"","\"tripId\":\""+trip+"\",\"quotationId\":\""+quote+"\",\"notes\"");
        JsonNode saved=create(jwt,payload,UUID.randomUUID());assertThat(saved.path("tripId").asText()).isEqualTo(trip.toString());assertThat(saved.path("quotationId").asText()).isEqualTo(quote);
        JsonNode refs=call(jwt,get("/work/references").param("customerId",person),200);
        assertThat(refs.path("trips").get(0).path("name").asText()).contains("F01REF");assertThat(refs.path("quotations").get(0).path("name").asText()).contains("Viagem F01");
        call(jwt,body(post("/work/tasks"),payload.replace(person,another)).header("Idempotency-Key",UUID.randomUUID()),400);
        call(jwt,body(post("/work/tasks"),payload.replace("TRAVEL","MILES")).header("Idempotency-Key",UUID.randomUUID()),400);
        assertThat(call(jwt,get("/work/tasks").param("customerId",person).param("product","MILES"),200)).isEmpty();
        assertThat(call(jwt,get("/work/references").param("customerId",another),200).path("trips")).isEmpty();
    }
    @Test void authorizedAgencyDeletionRemovesWorkAndPreservesOtherAgency()throws Exception{
        String own=http.loginOwner(mockMvc),ownPerson=person(own);
        String retained=create(own,task(ownPerson,"Preservada",0),UUID.randomUUID()).path("id").asText();
        Agency agency=agencies.save(Agency.builder().name("Excluir F01").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.ACTIVE).build());
        String owner=member(agency,AccountKind.AGENCY_OWNER),person=person(owner);
        String model=call(owner,body(post("/work/templates"),template("Descarte","ONCE",0)),201).path("id").asText();
        call(owner,body(post("/work/templates/"+model+"/apply"),"{\"customerId\":\""+person+"\",\"date\":\"2026-10-20\"}"),200);
        agency.setStatus(AgencyStatus.DELETION_PENDING);agency.setDeletionScheduledAt(java.time.Instant.now().minusSeconds(60));agencies.save(agency);
        deletionScheduler.executeScheduledDeletions();
        for(String table:List.of("work_tasks","work_task_events","checklist_runs","checklist_templates"))assertThat(jdbc.queryForObject("SELECT count(*) FROM "+table+" WHERE agency_id=?",Long.class,agency.getId())).isZero();
        assertThat(call(own,get("/work/tasks/"+retained),200).path("title").asText()).isEqualTo("Preservada");
    }
}
