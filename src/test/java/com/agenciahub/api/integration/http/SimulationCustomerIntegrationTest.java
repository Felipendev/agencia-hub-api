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

class SimulationCustomerIntegrationTest extends AbstractIntegrationTest {
    static final String INPUT="""
        {"title":"Plano pessoal","monthlySpending":10000,"pointsPerDollar":2,"exchangeRate":5,"exchangeDate":"2026-01-01",
        "months":12,"passengers":1,"milesPerPassenger":80000,"destination":"Lisboa","scenarios":[{"program":"Smiles","bonusPercent":100}]}
        """;
    @Autowired JdbcTemplate jdbc;
    @Autowired AgencyRepository agencies;
    @Autowired CrmCustomerRepository customers;
    @Autowired PlatformAccountRepository accounts;
    @Autowired PasswordEncoder passwords;
    @Autowired com.agenciahub.api.scheduling.AgencyDeletionScheduler scheduler;
    private JsonNode call(String jwt,MockHttpServletRequestBuilder request,int expected)throws Exception{
        String content=mockMvc.perform(request.header("Authorization","Bearer "+jwt)).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return content.isEmpty()?null:http.parse(content);
    }
    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request,String value){return request.contentType(MediaType.APPLICATION_JSON).content(value);}
    private String person(String jwt)throws Exception{return call(jwt,body(post("/customers"),"{\"name\":\"Cliente "+UUID.randomUUID()+"\",\"status\":\"ACTIVE\"}"),201).path("id").asText();}
    private String create(String jwt,String customer)throws Exception{return call(jwt,body(post("/accumulation-simulations"),"{\"customerId\":\""+customer+"\",\"assumptions\":"+INPUT+"}"),201).path("id").asText();}
    private String patchBody(String customer,int version){return "{\"customerId\":\""+customer+"\",\"expectedVersion\":"+version+",\"assumptions\":"+INPUT+"}";}
    private UUID ownAgency(String person){return customers.findById(UUID.fromString(person)).orElseThrow().getAgency().getId();}
    private String member(Agency agency,AccountKind kind)throws Exception{
        String email="plan-"+UUID.randomUUID()+"@test.local";
        accounts.save(PlatformAccount.builder().agency(agency).accountKind(kind).name("Autor do plano").email(email).passwordHash(passwords.encode("test1234")).active(true).emailVerified(true).mustChangePassword(false).build());
        return call("",body(post("/auth/login"),"{\"email\":\""+email+"\",\"password\":\"test1234\"}"),200).path("token").asText();
    }
    @Test void linkedCrudFiltersAndVersionCustomerPersistWithoutRewritingPriorVersions()throws Exception{
        String jwt=http.loginOwner(mockMvc),a=person(jwt),b=person(jwt),id=create(jwt,a);
        JsonNode initial=call(jwt,get("/accumulation-simulations/"+id),200);assertThat(initial.path("customerId").asText()).isEqualTo(a);
        assertThat(call(jwt,get("/accumulation-simulations").param("customerId",b),200)).isEmpty();
        JsonNode moved=call(jwt,body(patch("/accumulation-simulations/"+id),patchBody(b,1)),200);
        assertThat(moved.path("id").asText()).isEqualTo(id);assertThat(moved.path("version").asInt()).isEqualTo(2);assertThat(moved.path("customerId").asText()).isEqualTo(b);
        assertThat(call(jwt,get("/accumulation-simulations").param("customerId",a),200)).isEmpty();
        assertThat(call(jwt,get("/accumulation-simulations").param("customerId",b),200).get(0).path("id").asText()).isEqualTo(id);
        JsonNode history=call(jwt,get("/accumulation-simulations/"+id+"/history"),200);
        assertThat(history.get(0).path("customerId").asText()).isEqualTo(b);assertThat(history.get(1).path("customerId").asText()).isEqualTo(a);
        String actor=jdbc.queryForObject("SELECT id::text FROM users WHERE email=?",String.class,IntegrationHttpSupport.SEED_OWNER_EMAIL);
        assertThat(history.get(0).path("author").asText()).isEqualTo(actor);assertThat(history.get(0).path("authorName").asText()).isNotBlank();
        call(jwt,body(patch("/accumulation-simulations/"+id),patchBody(b,1)),409);
        call(jwt,delete("/accumulation-simulations/"+id).param("expectedVersion","1"),409);
        call(jwt,delete("/accumulation-simulations/"+id).param("expectedVersion","2"),204);
        call(jwt,get("/accumulation-simulations/"+id),404);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM accumulation_simulation_versions WHERE simulation_id=?::uuid",Long.class,id)).isZero();
    }
    @Test void legacyRowsStayUnlinkedUntilExplicitAssociationAndKeepLegacyHistory()throws Exception{
        String jwt=http.loginOwner(mockMvc),person=person(jwt);UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO accumulation_simulations(id,agency_id,title,assumptions,created_by) VALUES(?,?,?,?::jsonb,?)",id,ownAgency(person),"Plano pessoal",INPUT,"autor-legado");
        jdbc.update("INSERT INTO accumulation_simulation_versions(id,simulation_id,version,assumptions,author) VALUES(?,?,1,?::jsonb,?)",UUID.randomUUID(),id,INPUT,"autor-legado");
        assertThat(call(jwt,get("/accumulation-simulations/"+id),200).path("customerId").isNull()).isTrue();
        assertThat(call(jwt,get("/accumulation-simulations").param("unlinked","true"),200).toString()).contains(id.toString());
        call(jwt,body(patch("/accumulation-simulations/"+id),"{\"expectedVersion\":1,\"assumptions\":"+INPUT+"}"),400);
        call(jwt,body(patch("/accumulation-simulations/"+id),patchBody(person,1)),200);
        JsonNode history=call(jwt,get("/accumulation-simulations/"+id+"/history"),200);
        assertThat(history.get(0).path("customerId").asText()).isEqualTo(person);assertThat(history.get(1).path("customerId").isNull()).isTrue();
        assertThat(history.get(1).path("author").asText()).isEqualTo("autor-legado");assertThat(history.get(1).path("authorName").isNull()).isTrue();
        assertThat(call(jwt,get("/accumulation-simulations").param("unlinked","true"),200).toString()).doesNotContain(id.toString());
    }
    @Test void customerRequiredAndOldPatchCannotClearStoredAssociation()throws Exception{
        String jwt=http.loginOwner(mockMvc),person=person(jwt);
        call(jwt,body(post("/accumulation-simulations"),INPUT),400);
        call(jwt,body(post("/accumulation-simulations"),"{\"assumptions\":"+INPUT+"}"),400);
        String id=create(jwt,person);
        JsonNode updated=call(jwt,body(patch("/accumulation-simulations/"+id),"{\"expectedVersion\":1,\"assumptions\":"+INPUT.replace("10000","20000")+"}"),200);
        assertThat(updated.path("customerId").asText()).isEqualTo(person);
        call(jwt,get("/accumulation-simulations").param("customerId",person).param("unlinked","true"),400);
    }
    @Test void tenantAndRolesProtectEveryReadMutationAndFilter()throws Exception{
        String jwt=http.loginOwner(mockMvc),person=person(jwt),id=create(jwt,person);
        Agency own=customers.findById(UUID.fromString(person)).orElseThrow().getAgency();String seller=member(own,AccountKind.SALES_AGENT);
        call(seller,get("/accumulation-simulations/"+id),200);create(seller,person);
        call(member(own,AccountKind.PLATFORM_ADMIN),get("/accumulation-simulations"),403);
        mockMvc.perform(get("/accumulation-simulations")).andExpect(IntegrationHttpSupport.unauthenticated());
        Agency other=agencies.save(Agency.builder().name("Outra agência de planos").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.ACTIVE).build());
        String foreign=member(other,AccountKind.AGENCY_OWNER),otherPerson=person(foreign);
        call(foreign,get("/accumulation-simulations/"+id),404);call(foreign,get("/accumulation-simulations/"+id+"/history"),404);
        call(foreign,body(patch("/accumulation-simulations/"+id),patchBody(otherPerson,1)),404);call(foreign,delete("/accumulation-simulations/"+id),404);
        call(jwt,get("/accumulation-simulations").param("customerId",otherPerson),404);
        call(jwt,body(post("/accumulation-simulations"),"{\"customerId\":\""+otherPerson+"\",\"assumptions\":"+INPUT+"}"),404);
        call(jwt,body(patch("/accumulation-simulations/"+id),patchBody(otherPerson,1)),404);
        assertThat(call(foreign,get("/accumulation-simulations"),200)).isEmpty();
    }
    @Test void concurrentRelinksRespectVersionAndKeepOriginalLinkInHistory()throws Exception{
        String jwt=http.loginOwner(mockMvc),a=person(jwt),b=person(jwt),c=person(jwt),id=create(jwt,a);
        var executor=Executors.newFixedThreadPool(2);
        try{
            List<Callable<Integer>> edits=new ArrayList<>();for(String target:List.of(b,c))edits.add(()->mockMvc.perform(body(patch("/accumulation-simulations/"+id),patchBody(target,1)).header("Authorization","Bearer "+jwt)).andReturn().getResponse().getStatus());
            var responses=executor.invokeAll(edits);assertThat(List.of(responses.get(0).get(),responses.get(1).get())).containsExactlyInAnyOrder(200,409);
        }finally{executor.shutdownNow();}
        JsonNode history=call(jwt,get("/accumulation-simulations/"+id+"/history"),200);assertThat(history.size()).isEqualTo(2);assertThat(history.get(1).path("customerId").asText()).isEqualTo(a);
    }
    @Test void movedPlansStillBlockDeletionAndPartialAnonymizationOfHistoricalCustomer()throws Exception{
        String jwt=http.loginOwner(mockMvc),a=person(jwt),b=person(jwt),id=create(jwt,a),email="history-plan-"+UUID.randomUUID()+"@test.local";
        call(jwt,body(patch("/customers/"+a),"{\"email\":\""+email+"\"}"),200);
        call(jwt,body(patch("/accumulation-simulations/"+id),patchBody(b,1)),200);
        assertThat(call(jwt,delete("/customers/"+a),409).path("code").asText()).isEqualTo("SIMULATION_CUSTOMER_HISTORY_EXISTS");
        String request=call(jwt,body(post("/public/data-deletion-request"),"{\"email\":\""+email+"\"}"),201).path("requestId").asText();
        JsonNode blocked=call(jwt,body(post("/agency/data-deletion-requests/"+request+"/process"),"{\"acao\":\"EXCLUIR\"}"),400);
        assertThat(blocked.path("message").asText()).contains("continua pendente");assertThat(jdbc.queryForObject("SELECT status FROM data_deletion_requests WHERE id=?::uuid",String.class,request)).isEqualTo("PENDING");
    }
    @Test void savingAndReadingPlansDoNotEnrollWalletOrChangeCommercialData()throws Exception{
        String jwt=http.loginOwner(mockMvc),person=person(jwt);Map<String,Long> counts=new HashMap<>();
        for(String table:List.of("customer_miles_profiles","customer_miles_items","customer_miles_movements","sales","financial_entries","trips"))counts.put(table,jdbc.queryForObject("SELECT count(*) FROM "+table,Long.class));
        create(jwt,person);call(jwt,get("/accumulation-simulations").param("customerId",person),200);
        for(var count:counts.entrySet())assertThat(jdbc.queryForObject("SELECT count(*) FROM "+count.getKey(),Long.class)).isEqualTo(count.getValue());
    }
    @Test void authorizedAgencyDeletionCleansPlansVersionsAndPreservesOtherAgency()throws Exception{
        String jwt=http.loginOwner(mockMvc),person=person(jwt),retained=create(jwt,person);
        Agency agency=agencies.save(Agency.builder().name("Excluir planos").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.ACTIVE).build());
        String owner=member(agency,AccountKind.AGENCY_OWNER),otherPerson=person(owner),id=create(owner,otherPerson);
        agency.setStatus(AgencyStatus.DELETION_PENDING);agency.setDeletionScheduledAt(java.time.Instant.now().minusSeconds(60));agencies.save(agency);scheduler.executeScheduledDeletions();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM accumulation_simulations WHERE id=?::uuid",Long.class,id)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM accumulation_simulation_versions WHERE simulation_id=?::uuid",Long.class,id)).isZero();
        call(jwt,get("/accumulation-simulations/"+retained),200);
    }
}
