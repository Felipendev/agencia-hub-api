package com.agenciahub.api.integration.http;

import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.application.persistence.entity.*;
import com.agenciahub.api.application.persistence.repository.*;
import com.agenciahub.api.domain.enums.*;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActivityHistoryIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AgencyRepository agencies;
    @Autowired PlatformAccountRepository users;
    @Autowired PasswordEncoder passwords;
    String jwt; UUID person,agency,user;
    static final String SECRET="PRIVATE_CONTACT_DOCUMENT_TOKEN";
    @BeforeEach void setup() throws Exception {
        jwt=http.loginOwner(mockMvc);
        person=UUID.fromString(call(jwt,post("/customers").contentType(MediaType.APPLICATION_JSON).content(
                "{\"name\":\"Histórico\",\"email\":\"h-"+UUID.randomUUID()+"@test.local\",\"status\":\"ACTIVE\"}"),201).path("id").asText());
        agency=jdbc.queryForObject("SELECT agency_id FROM customers WHERE id=?",UUID.class,person);
        user=jdbc.queryForObject("SELECT id FROM users WHERE email=?",UUID.class,http.SEED_OWNER_EMAIL);
    }
    JsonNode call(String token,MockHttpServletRequestBuilder req,int code)throws Exception{
        return http.parse(mockMvc.perform(req.header("Authorization","Bearer "+token)).andExpect(status().is(code)).andReturn().getResponse().getContentAsString());
    }
    MockHttpServletRequestBuilder query(String path,String product,String... modules){
        return get(path).param("product",product).param("from","2026-10-20").param("to","2026-10-20")
                .param("customerId",person.toString()).param("modules",modules);
    }
    void wallet(String action,String actor,String at){
        jdbc.update("INSERT INTO customer_miles_audit(id,agency_id,customer_id,resource_id,action,snapshot,author,created_at) VALUES(?,?,?,?,?,?::jsonb,?,?::timestamptz)",
                UUID.randomUUID(),agency,person,UUID.randomUUID(),action,"{\"secret\":\""+SECRET+"\"}",actor,at);
    }
    @Test void mergesEveryAvailableSourceWithoutSnapshotsOrFinancialFields()throws Exception{
        String detail="{\"product\":\"TRAVEL\",\"customerId\":\""+person+"\",\"participantReferrerId\":\""+person+"\",\"secret\":\""+SECRET+"\"}";
        UUID resource=UUID.randomUUID();
        wallet("CREATED",user.toString(),"2026-10-20T12:00:00Z");
        jdbc.update("INSERT INTO contract_audit(id,agency_id,resource_type,resource_id,customer_id,action,detail,author,created_at) VALUES(?,?,'CONTRACT',?,?,'UPDATED',?::jsonb,?, '2026-10-20T12:00:00Z')",UUID.randomUUID(),agency,resource,person,detail,user.toString());
        jdbc.update("INSERT INTO appointment_events(id,agency_id,appointment_id,customer_id,action,detail,author,created_at) VALUES(?,?,?,?,'DELETED',?::jsonb,?,'2026-10-20T12:00:00Z')",UUID.randomUUID(),agency,resource,person,detail,user.toString());
        jdbc.update("INSERT INTO work_task_events(id,agency_id,resource_type,resource_id,customer_id,action,snapshot,author_id,created_at) VALUES(?,?,'TASK',?,?,'DONE',?::jsonb,?,'2026-10-20T12:00:00Z')",UUID.randomUUID(),agency,resource,person,detail,user);
        jdbc.update("INSERT INTO trip_savings_audit(id,agency_id,resource_type,resource_id,customer_id,action,detail,author,created_at) VALUES(?,?,'SAVINGS',?,?,'DELETED',?::jsonb,?,'2026-10-20T12:00:00Z')",UUID.randomUUID(),agency,resource,person,detail,user.toString());
        jdbc.update("INSERT INTO trip_stage_events(id,agency_id,trip_id,stage_id,action,detail,author,created_at) VALUES(?,?,?,?,'DELETED',?::jsonb,?,'2026-10-20T12:00:00Z')",UUID.randomUUID(),agency,UUID.randomUUID(),resource,detail,user.toString());
        jdbc.update("INSERT INTO referral_events(id,agency_id,referral_id,action,detail,author,created_at) VALUES(?,?,?,'DELETED',?::jsonb,?,'2026-10-20T12:00:00Z')",UUID.randomUUID(),agency,resource,detail,user.toString());
        jdbc.update("INSERT INTO commercial_followup_events(id,agency_id,followup_id,customer_id,action,snapshot,author_id,created_at) VALUES(?,?,?,?,'DELETED',?::jsonb,?,'2026-10-20T12:00:00Z')",UUID.randomUUID(),agency,resource,person,detail,user);
        jdbc.update("INSERT INTO accumulation_simulations(id,agency_id,title,assumptions,created_by,customer_id) VALUES(?,?,'Plano',?::jsonb,?,?)",resource,agency,detail,user.toString(),person);
        jdbc.update("INSERT INTO accumulation_simulation_versions(id,simulation_id,version,assumptions,author,customer_id,recorded_at) VALUES(?,?,1,?::jsonb,?,?,'2026-10-20T12:00:00Z')",UUID.randomUUID(),resource,detail,user.toString(),person);
        jdbc.update("INSERT INTO simulation_task_generations(id,agency_id,source_id,customer_id,source_version,source_title,snapshot,request_payload,author_id,created_at) VALUES(?,?,?,?,1,'Plano',?::jsonb,'{}',?,'2026-10-20T12:00:00Z')",UUID.randomUUID(),agency,resource,person,detail,user);
        jdbc.update("INSERT INTO miles_diagnostics(id,agency_id,request_key,original_data,data,source,customer_id,receipt,applied_input) VALUES(?,?,?,'{}','{}','STAFF',?,'{}','{}')",resource,agency,UUID.randomUUID(),person);
        jdbc.update("INSERT INTO miles_diagnostic_events(id,agency_id,diagnostic_id,action,author_id,snapshot,created_at) VALUES(?,?,?,'INCORPORATED',?,?::jsonb,'2026-10-20T12:00:00Z')",UUID.randomUUID(),agency,resource,user,detail);
        var miles=call(jwt,query("/activity-history","MILES","WALLET","SIMULATIONS","PLAN_TASKS","DIAGNOSTICS"),200);
        var travel=call(jwt,query("/activity-history","TRAVEL","CONTRACTS","AGENDA","TASKS","TRIP_STAGES","SAVINGS","REFERRALS","FOLLOWUPS"),200);
        assertThat(miles.path("events")).hasSize(4); assertThat(travel.path("events")).hasSize(7);
        assertThat(miles.toString()+travel).doesNotContain(SECRET,"snapshot","email","price","assumptions");
        assertThat(travel.path("events").toString()).contains("DELETED",user.toString());
        var only=call(jwt,query("/activity-history","TRAVEL","AGENDA"),200);
        assertThat(only.path("events")).hasSize(1);
        assertThat(call(jwt,query("/activity-history","MILES","AGENDA"),200).path("events")).isEmpty();
    }
    @Test void inclusiveDaysUseFortalezaAndLegacyAuthorsAreNotGuessed()throws Exception{
        wallet("BEFORE","legacy-contact@test.local","2026-10-20T02:59:59Z");
        wallet("START","legacy-contact@test.local","2026-10-20T03:00:00Z");
        wallet("END",user.toString(),"2026-10-21T02:59:59Z");
        wallet("AFTER",user.toString(),"2026-10-21T03:00:00Z");
        var result=call(jwt,query("/activity-history","MILES","WALLET"),200);
        assertThat(result.path("events")).hasSize(2);
        assertThat(result.toString()).contains("START","END","Autor não identificado").doesNotContain("BEFORE","AFTER","legacy-contact");
        assertThat(result.path("events").get(1).path("authorId").isNull()).isTrue();
        assertThat(result.path("timeZone").asText()).isEqualTo("America/Fortaleza");
    }
    @Test void unfilteredDiagnosticHistoryAndCsvIncludeOnlyIncorporatedDeclarations()throws Exception{
        UUID pending=UUID.randomUUID(),incorporated=UUID.randomUUID();
        jdbc.update("INSERT INTO miles_diagnostics(id,agency_id,request_key,original_data,data,source) VALUES(?,?,?,'{}','{}','STAFF')",pending,agency,UUID.randomUUID());
        jdbc.update("INSERT INTO miles_diagnostics(id,agency_id,request_key,original_data,data,source,customer_id,receipt,applied_input) VALUES(?,?,?,'{}','{}','STAFF',?,'{}','{}')",incorporated,agency,UUID.randomUUID(),person);
        for(UUID id:List.of(pending,incorporated))
            jdbc.update("INSERT INTO miles_diagnostic_events(id,agency_id,diagnostic_id,action,author_id,snapshot,created_at) VALUES(?,?,?,'CREATED',?,'{}','2026-10-20T12:00:00Z')",UUID.randomUUID(),agency,id,user);
        var request=query("/activity-history","MILES","DIAGNOSTICS").with(r->{r.removeParameter("customerId");return r;});
        String result=call(jwt,request,200).toString();
        assertThat(result).contains(incorporated.toString()).doesNotContain(pending.toString());
        String csv=mockMvc.perform(query("/activity-history/export.csv","MILES","DIAGNOSTICS")
                .with(r->{r.removeParameter("customerId");return r;}).header("Authorization","Bearer "+jwt))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(csv).contains(incorporated.toString()).doesNotContain(pending.toString());
    }
    @Test void rejectsForeignPeopleAndAnonymousOrPlatformReadersAndSellerExports()throws Exception{
        wallet("OWN",user.toString(),"2026-10-20T12:00:00Z");
        Agency other=agencies.save(Agency.builder().name("Outra agência").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.TRIAL).build());
        String seller=member(agencies.findById(agency).orElseThrow(),AccountKind.SALES_AGENT);
        call(seller,query("/activity-history","MILES","WALLET"),200);
        mockMvc.perform(query("/activity-history/export.csv","MILES","WALLET").header("Authorization","Bearer "+seller)).andExpect(status().isForbidden());
        call(member(other,AccountKind.AGENCY_OWNER),query("/activity-history","MILES","WALLET"),404);
        mockMvc.perform(query("/activity-history","MILES","WALLET")).andExpect(status().isForbidden());
        mockMvc.perform(query("/activity-history","MILES","WALLET").header("Authorization","Bearer "+member(null,AccountKind.PLATFORM_ADMIN))).andExpect(status().isForbidden());
        // A corrupt legacy row in another tenant must never leak even when its person ID matches.
        jdbc.update("INSERT INTO customer_miles_audit(id,agency_id,customer_id,resource_id,action,snapshot,author,created_at) VALUES(?,?,?,?,'FOREIGN','{}',?,'2026-10-20T12:00:00Z')",UUID.randomUUID(),other.getId(),person,UUID.randomUUID(),user.toString());
        assertThat(call(jwt,query("/activity-history","MILES","WALLET"),200).path("events")).hasSize(1);
    }
    @Test void stablePaginationAndExportBoundRejectPartialFile()throws Exception{
        jdbc.update("INSERT INTO customer_miles_audit(id,agency_id,customer_id,resource_id,action,snapshot,author,created_at) SELECT gen_random_uuid(),?,?,gen_random_uuid(),'CREATED','{}',?,'2026-10-20T12:00:00Z' FROM generate_series(1,102)",agency,person,user.toString());
        var one=call(jwt,query("/activity-history","MILES","WALLET"),200);
        var two=call(jwt,query("/activity-history","MILES","WALLET").param("page","1"),200);
        assertThat(one.path("events")).hasSize(100);assertThat(one.path("hasMore").asBoolean()).isTrue();
        assertThat(two.path("events")).hasSize(2);assertThat(two.path("hasMore").asBoolean()).isFalse();
        Set<String> ids=new HashSet<>();one.path("events").forEach(e->ids.add(e.path("id").asText()));
        two.path("events").forEach(e->assertThat(ids).doesNotContain(e.path("id").asText()));
        assertThat(call(jwt,query("/activity-history","MILES","WALLET"),200)).isEqualTo(one);
        jdbc.update("INSERT INTO customer_miles_audit(id,agency_id,customer_id,resource_id,action,snapshot,author,created_at) SELECT gen_random_uuid(),?,?,gen_random_uuid(),'CREATED','{}',?,'2026-10-20T12:00:00Z' FROM generate_series(1,4900)",agency,person,user.toString());
        mockMvc.perform(query("/activity-history/export.csv","MILES","WALLET").header("Authorization","Bearer "+jwt)).andExpect(status().isUnprocessableEntity());
    }
    @Test void validatesRangeModulesAndPaginationBeforeQuery()throws Exception{
        call(jwt,query("/activity-history","MILES","WALLET").with(r->{r.setParameter("from","2026-10-21");return r;}),400);
        call(jwt,query("/activity-history","MILES","WALLET").with(r->{r.setParameter("from","2025-01-01");return r;}),400);
        call(jwt,query("/activity-history","MILES","WALLET").param("page","-1"),400);
        call(jwt,query("/activity-history","MILES","WALLET").param("page","50"),400);
        call(jwt,query("/activity-history","MILES","UNKNOWN"),400);
        call(jwt,get("/activity-history").param("product","MILES").param("from","2026-10-01").param("to","2026-10-20"),400);
    }
    @Test void csvUsesSameFiltersNeutralizesFormulasAndNeverChangesSource()throws Exception{
        wallet(" =SUM(1;2)","private-email@test.local","2026-10-20T12:00:00Z");
        wallet("QUOTE_\"_LINE\nSAFE",user.toString(),"2026-10-20T12:00:00Z");
        var before=jdbc.queryForList("SELECT * FROM customer_miles_audit WHERE customer_id=? ORDER BY id",person);
        var response=mockMvc.perform(query("/activity-history/export.csv","MILES","WALLET").header("Authorization","Bearer "+jwt))
                .andExpect(status().isOk()).andReturn().getResponse();
        String csv=response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFFProduto;").contains("\"' =SUM(1;2)\"","QUOTE_\"\"_LINE","-03:00").doesNotContain(SECRET,"private-email");
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(jdbc.queryForList("SELECT * FROM customer_miles_audit WHERE customer_id=? ORDER BY id",person)).isEqualTo(before);
        assertThat(mockMvc.perform(query("/activity-history/export.csv","TRAVEL","WALLET").header("Authorization","Bearer "+jwt)).andReturn().getResponse().getContentAsString()).doesNotContain("SUM");
    }
    String member(Agency agency,AccountKind kind)throws Exception{
        String email="history-"+UUID.randomUUID()+"@test.local";
        users.save(PlatformAccount.builder().agency(agency).accountKind(kind).name("Equipe").email(email)
                .passwordHash(passwords.encode("history-test")).active(true).emailVerified(true).termsAccepted(true).build());
        return call("",post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"history-test\"}"),200).path("token").asText();
    }
}
