package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.*;
import com.agenciahub.api.application.persistence.repository.*;
import com.agenciahub.api.domain.*;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SimulationTasksIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AgencyRepository agencies;
    @Autowired PlatformAccountRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired com.agenciahub.api.scheduling.AgencyDeletionScheduler scheduler;
    private static final String ASSUMPTIONS=SimulationCustomerIntegrationTest.INPUT;
    private JsonNode call(String jwt,MockHttpServletRequestBuilder request,int expected)throws Exception{
        String body=mockMvc.perform(request.header("Authorization","Bearer "+jwt)).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return body.isEmpty()?null:http.parse(body);
    }
    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder r,String value){return r.contentType(MediaType.APPLICATION_JSON).content(value);}
    private String person(String jwt)throws Exception{return call(jwt,body(post("/customers"),"{\"name\":\"Pessoa tarefas "+UUID.randomUUID()+"\",\"email\":\"plan-task-"+UUID.randomUUID()+"@test.local\",\"status\":\"ACTIVE\"}"),201).path("id").asText();}
    private String plan(String jwt,String customer)throws Exception{return call(jwt,body(post("/accumulation-simulations"),"{\"customerId\":\""+customer+"\",\"assumptions\":"+ASSUMPTIONS+"}"),201).path("id").asText();}
    private String responsible(String person){return jdbc.queryForObject("SELECT id::text FROM users WHERE agency_id=(SELECT agency_id FROM customers WHERE id=?::uuid) AND account_kind='AGENCY_OWNER' LIMIT 1",String.class,person);}
    private String path(String id){return "/accumulation-simulations/"+id+"/task-generation";}
    private String input(String person,String owner){return "{\"expectedVersion\":1,\"customerId\":\""+person+"\",\"responsibleId\":\""+owner+"\",\"steps\":[{\"title\":\"Revisar meta\",\"notes\":\"Conferir premissas\",\"dueOn\":\"2026-11-01\"},{\"title\":\"Acompanhar acúmulo\",\"dueOn\":\"2026-12-01\"}]}";}
    private JsonNode generate(String jwt,String id,String input)throws Exception{return call(jwt,body(post(path(id)),input),200);}
    private String edit(JsonNode t,String title,String customer){return "{\"product\":\"MILES\",\"customerId\":\""+customer+"\",\"title\":\""+title+"\",\"notes\":\"Revisada\",\"dueOn\":\"2026-11-03\",\"responsibleId\":\""+t.path("responsibleId").asText()+"\",\"expectedVersion\":"+t.path("version").asInt()+"}";}
    @Test void generationPersistsWithSnapshotAndTasksHaveFullCrudWithoutAnotherBatch()throws Exception{
        String jwt=http.loginOwner(mockMvc),person=person(jwt),id=plan(jwt,person),input=input(person,responsible(person));
        assertThat(call(jwt,get(path(id)),200).path("generation").isNull()).isTrue();
        Map<String,Long> before=new HashMap<>();for(String table:List.of("customer_miles_profiles","customer_miles_movements","sales","financial_entries","trips"))before.put(table,jdbc.queryForObject("SELECT count(*) FROM "+table,Long.class));
        JsonNode receipt=generate(jwt,id,input),task=receipt.path("tasks").get(0);String taskId=task.path("id").asText();
        assertThat(receipt.path("tasks").size()).isEqualTo(2);assertThat(task.path("product").asText()).isEqualTo("MILES");assertThat(task.path("fromSimulation").asBoolean()).isTrue();assertThat(task.path("simulationId").asText()).isEqualTo(id);
        assertThat(receipt.path("snapshot").path("result").path("basePoints").asLong()).isEqualTo(48000);assertThat(receipt.path("authorName").asText()).isNotBlank();
        assertThat(call(jwt,get(path(id)),200).path("generation").path("id")).isEqualTo(receipt.path("id"));
        assertThat(generate(jwt,id,input).path("id")).isEqualTo(receipt.path("id"));
        call(jwt,body(post(path(id)),input.replace("Revisar meta","Outra ação")),409);
        JsonNode updated=call(jwt,body(patch("/work/tasks/"+taskId),edit(task,"Meta revisada",person)),200);
        assertThat(updated.path("id")).isEqualTo(task.path("id"));assertThat(updated.path("version").asInt()).isEqualTo(2);
        call(jwt,body(patch("/work/tasks/"+taskId),edit(task,"Versão antiga",person)),409);
        for(String state:List.of("DONE","OPEN","CANCELLED","OPEN"))updated=call(jwt,body(post("/work/tasks/"+taskId+"/state"),"{\"state\":\""+state+"\",\"expectedVersion\":"+updated.path("version").asInt()+",\"reason\":\"Revisão de teste\"}"),200);
        call(jwt,delete("/work/tasks/"+taskId).param("expectedVersion",updated.path("version").asText()),204);
        JsonNode context=call(jwt,get(path(id)),200);assertThat(context.path("generation").path("tasks").get(0).path("deleted").asBoolean()).isTrue();
        assertThat(context.path("generation").path("requested").path("steps").get(0).path("title").asText()).isEqualTo("Revisar meta");
        assertThat(context.path("generation").path("tasks").get(0).path("title").asText()).isEqualTo("Meta revisada");
        assertThat(generate(jwt,id,input).path("tasks").get(0).path("deleted").asBoolean()).isTrue();
        assertThat(call(jwt,get("/work/history/TASK/"+taskId),200).toString()).contains("PLAN_LINKED","sourceVersion");
        for(var count:before.entrySet())assertThat(jdbc.queryForObject("SELECT count(*) FROM "+count.getKey(),Long.class)).isEqualTo(count.getValue());
    }
    @Test void planEditsRelinkingAndDeletionNeverRewriteOrDuplicateExistingTasks()throws Exception{
        String jwt=http.loginOwner(mockMvc),a=person(jwt),b=person(jwt),id=plan(jwt,a),input=input(a,responsible(a));JsonNode receipt=generate(jwt,id,input);String taskId=receipt.path("tasks").get(0).path("id").asText();
        call(jwt,body(patch("/accumulation-simulations/"+id),"{\"customerId\":\""+b+"\",\"expectedVersion\":1,\"assumptions\":"+ASSUMPTIONS.replace("10000","20000")+"}"),200);
        JsonNode after=call(jwt,get(path(id)),200);assertThat(after.path("simulation").path("customerId").asText()).isEqualTo(b);assertThat(after.path("generation").path("snapshot").path("customerId").asText()).isEqualTo(a);
        assertThat(after.path("generation").path("snapshot").path("result").path("basePoints").asLong()).isEqualTo(48000);
        assertThat(generate(jwt,id,input).path("id")).isEqualTo(receipt.path("id"));
        call(jwt,body(post(path(id)),input(b,responsible(b)).replace("\"expectedVersion\":1","\"expectedVersion\":2")),409);
        call(jwt,body(patch("/work/tasks/"+taskId),edit(receipt.path("tasks").get(0),"Não mover",b)),400);
        assertThatThrownBy(()->jdbc.update("UPDATE work_tasks SET customer_id=?::uuid WHERE id=?::uuid",b,taskId)).hasMessageContaining("pessoa e produto");
        call(jwt,delete("/accumulation-simulations/"+id).param("expectedVersion","2"),204);
        JsonNode archived=call(jwt,get(path(id)),200);assertThat(archived.path("simulation").isNull()).isTrue();assertThat(archived.path("generation").path("tasks").size()).isEqualTo(2);
        assertThat(call(jwt,get("/accumulation-simulations/task-generations"),200).toString()).contains(id,"\"planDeleted\":true");call(jwt,body(post(path(id)),input),404);
        JsonNode retained=call(jwt,get("/work/tasks/"+taskId),200);assertThat(retained.path("simulationId").isNull()).isTrue();assertThat(retained.path("fromSimulation").asBoolean()).isTrue();
        assertThat(retained.path("simulationSourceId").asText()).isEqualTo(id);
        assertThat(call(jwt,get("/work/tasks").param("product","MILES").param("simulationId",id),200).size()).isEqualTo(2);
        assertThat(call(jwt,get("/work/tasks").param("product","TRAVEL").param("simulationId",id),200)).isEmpty();
    }
    @Test void staleVersionsLegacyAndInvalidFieldsDoNotCreateTasks()throws Exception{
        String jwt=http.loginOwner(mockMvc),p=person(jwt),id=plan(jwt,p),input=input(p,responsible(p));
        call(jwt,body(post(path(id)),input.replace("\"expectedVersion\":1","\"expectedVersion\":2")),409);
        for(String bad:List.of(input.replace("Revisar meta"," "),input.replace("2026-11-01","não-data"),input.replace("\"steps\":[","\"steps\":null,\"unused\":["),input.replace(p,UUID.randomUUID().toString())))call(jwt,body(post(path(id)),bad),bad.contains("unused")?400:bad.contains("não-data")||bad.contains("\" \"")?400:409);
        call(jwt,body(post(path(id)),input.replace(responsible(p),UUID.randomUUID().toString())),400);
        call(jwt,get(path("not-uuid")),400);
        var oversized=http.parse(input).deepCopy();
        var many=(com.fasterxml.jackson.databind.node.ArrayNode)oversized.path("steps");while(many.size()<21)many.add(many.get(0).deepCopy());
        call(jwt,body(post(path(id)),oversized.toString()),400);
        jdbc.update("UPDATE accumulation_simulations SET customer_id=NULL WHERE id=?::uuid",id);call(jwt,body(post(path(id)),input),409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM simulation_task_generations WHERE source_id=?::uuid",Long.class,id)).isZero();
    }
    @Test void concurrentConfirmationCreatesOneBatchAndTwoTasks()throws Exception{
        String jwt=http.loginOwner(mockMvc),p=person(jwt),id=plan(jwt,p),input=input(p,responsible(p));var pool=Executors.newFixedThreadPool(2);
        try{Callable<String> action=()->generate(jwt,id,input).path("id").asText();var results=pool.invokeAll(List.of(action,action));assertThat(results.get(0).get()).isEqualTo(results.get(1).get());}finally{pool.shutdownNow();}
        assertThat(jdbc.queryForObject("SELECT count(*) FROM simulation_task_generations WHERE source_id=?::uuid",Long.class,id)).isEqualTo(1);
        assertThat(call(jwt,get("/work/tasks").param("simulationId",id).param("product","MILES"),200).size()).isEqualTo(2);
    }
    @Test void failureOnSecondTaskRollsBackWholeGeneration()throws Exception{
        String jwt=http.loginOwner(mockMvc),p=person(jwt),id=plan(jwt,p),input=input(p,responsible(p));
        jdbc.execute("CREATE FUNCTION p65_reject_test_task() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.title='Recusar lote P65' THEN RAISE EXCEPTION 'falha de teste P65'; END IF; RETURN NEW; END $$");
        jdbc.execute("CREATE TRIGGER p65_atomicity_test BEFORE INSERT ON work_tasks FOR EACH ROW EXECUTE FUNCTION p65_reject_test_task()");
        try{call(jwt,body(post(path(id)),input.replace("Acompanhar acúmulo","Recusar lote P65")),500);}finally{jdbc.execute("DROP TRIGGER p65_atomicity_test ON work_tasks");jdbc.execute("DROP FUNCTION p65_reject_test_task()");}
        assertThat(call(jwt,get(path(id)),200).path("generation").isNull()).isTrue();
        assertThat(call(jwt,get("/work/tasks").param("customerId",p),200)).isEmpty();
        assertThat(generate(jwt,id,input).path("tasks").size()).isEqualTo(2);
    }
    private String member(Agency agency,AccountKind kind)throws Exception{
        String email="p65-"+UUID.randomUUID()+"@test.local";users.save(PlatformAccount.builder().agency(agency).accountKind(kind).name("Equipe P65").email(email).passwordHash(passwords.encode("test-p65-password")).active(true).emailVerified(true).termsAccepted(true).build());
        return call("unused",body(post("/auth/login"),"{\"email\":\""+email+"\",\"password\":\"test-p65-password\"}"),200).path("token").asText();
    }
    @Test void rolesAndTenantsProtectGenerationAndTaskFiltering()throws Exception{
        String jwt=http.loginOwner(mockMvc),p=person(jwt),id=plan(jwt,p),input=input(p,responsible(p));
        Agency own=agencies.findById(jdbc.queryForObject("SELECT agency_id FROM customers WHERE id=?::uuid",UUID.class,p)).orElseThrow();
        String seller=member(own,AccountKind.SALES_AGENT);generate(seller,id,input);
        Agency other=agencies.save(Agency.builder().name("Outra P65").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.TRIAL).build());String foreign=member(other,AccountKind.AGENCY_OWNER);
        call(foreign,get(path(id)),404);call(foreign,body(post(path(id)),input),404);assertThat(call(foreign,get("/work/tasks").param("simulationId",id),200)).isEmpty();
        assertThat(call(foreign,get("/accumulation-simulations/task-generations"),200)).isEmpty();
        String ownPlan=plan(jwt,p);String foreignPerson=person(foreign);call(jwt,body(post(path(ownPlan)),input(p,responsible(foreignPerson))),400);
        UUID inactive=users.save(PlatformAccount.builder().agency(own).accountKind(AccountKind.SALES_AGENT).name("Inativo").email("inactive-p65-"+UUID.randomUUID()+"@test.local").passwordHash("hash").active(false).build()).getId();
        call(jwt,body(post(path(ownPlan)),input(p,inactive.toString())),400);
        String admin=member(null,AccountKind.PLATFORM_ADMIN);call(admin,get(path(id)),403);call(admin,get("/accumulation-simulations/task-generations"),403);call(admin,body(post(path(id)),input),403);mockMvc.perform(get(path(id))).andExpect(status().isForbidden());
    }
    @Test void tasksKeepPrivacyReviewPendingAfterDeletingPlanAndTasks()throws Exception{
        String jwt=http.loginOwner(mockMvc),p=person(jwt),id=plan(jwt,p);JsonNode receipt=generate(jwt,id,input(p,responsible(p)));
        call(jwt,delete("/accumulation-simulations/"+id).param("expectedVersion","1"),204);for(JsonNode task:receipt.path("tasks"))call(jwt,delete("/work/tasks/"+task.path("id").asText()).param("expectedVersion","1"),204);
        JsonNode history=call(jwt,get(path(id)),200);assertThat(history.path("generation").path("tasks").size()).isEqualTo(2);for(JsonNode task:history.path("generation").path("tasks"))assertThat(task.path("deleted").asBoolean()).isTrue();
        call(jwt,delete("/customers/"+p),409);String email=jdbc.queryForObject("SELECT email FROM customers WHERE id=?::uuid",String.class,p);
        String request=call(jwt,body(post("/public/data-deletion-request"),"{\"email\":\""+email+"\"}"),201).path("requestId").asText();call(jwt,body(post("/agency/data-deletion-requests/"+request+"/process"),"{\"acao\":\"EXCLUIR\"}"),400);
        assertThat(jdbc.queryForObject("SELECT status FROM data_deletion_requests WHERE id=?::uuid",String.class,request)).isEqualTo("PENDING");
    }
    @Test void agencyDeletionRemovesOriginsBeforePeopleAndLeavesOtherAgencies()throws Exception{
        Agency doomed=agencies.save(Agency.builder().name("Descartar P65").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.TRIAL).build());String jwt=member(doomed,AccountKind.AGENCY_OWNER),p=person(jwt),id=plan(jwt,p);generate(jwt,id,input(p,responsible(p)));
        String keptJwt=http.loginOwner(mockMvc),keptPerson=person(keptJwt),keptPlan=plan(keptJwt,keptPerson);generate(keptJwt,keptPlan,input(keptPerson,responsible(keptPerson)));
        doomed.setStatus(AgencyStatus.DELETION_PENDING);doomed.setDeletionScheduledAt(Instant.now().minusSeconds(5));agencies.save(doomed);scheduler.executeScheduledDeletions();
        assertThat(agencies.findById(doomed.getId())).isEmpty();for(String table:List.of("simulation_task_links","simulation_task_generations","work_tasks"))assertThat(jdbc.queryForObject("SELECT count(*) FROM "+table+" WHERE agency_id=?",Long.class,doomed.getId())).isZero();
        assertThat(call(keptJwt,get(path(keptPlan)),200).path("generation").path("tasks").size()).isEqualTo(2);
    }
}
