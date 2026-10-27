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
import java.time.*;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReferralsLifecycleIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AgencyRepository agencies;
    @Autowired CrmCustomerRepository customers;
    @Autowired PlatformAccountRepository accounts;
    @Autowired AgencyDeletionScheduler scheduler;

    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder r,String text) {return r.contentType(MediaType.APPLICATION_JSON).content(text);}
    private JsonNode call(String jwt,MockHttpServletRequestBuilder r,int code)throws Exception {
        String text=mockMvc.perform(r.header("Authorization","Bearer "+jwt)).andExpect(status().is(code)).andReturn().getResponse().getContentAsString();
        return text.isEmpty()?null:http.parse(text);
    }
    private String person(String jwt)throws Exception {return call(jwt,body(post("/customers"),"{\"name\":\"Pessoa indicações "+UUID.randomUUID()+"\",\"status\":\"ACTIVE\"}"),201).path("id").asText();}
    private String input(String person,String email) {
        return "{\"product\":\"MILES\",\"referrerId\":\""+person+"\",\"referredName\":\"Contato\",\"referredPhone\":\"+351919123456\",\"referredEmail\":\""+email+"\",\"referredOn\":\""+LocalDate.now(ZoneId.of("America/Fortaleza")).minusDays(1)+"\",\"channel\":\"EVENT\",\"channelDetail\":\"Feira\",\"notes\":\"Preferência\"}";
    }
    private String create(String jwt,String payload)throws Exception {return call(jwt,body(post("/referrals").header("Idempotency-Key",UUID.randomUUID()),payload),201).path("referral").path("id").asText();}

    @Test void retriesCompareAllFieldsAndNoOpsRequireCurrentVersion()throws Exception {
        String jwt=http.loginOwner(mockMvc),referrer=person(jwt),referred=person(jwt),payload=input(referrer,"retry@example.test");UUID key=UUID.randomUUID();
        call(jwt,body(post("/referrals").header("Idempotency-Key",key),payload),201);
        for(String changed:java.util.List.of(payload.replace("351919123456","351919123457"),payload.replace("retry@example.test","other@example.test"),
                payload.replace("EVENT","WHATSAPP"),payload.replace("Feira","Conversa"),payload.replace("Preferência","Outro"))) {
            call(jwt,body(post("/referrals").header("Idempotency-Key",key),changed),409);
        }
        call(jwt,body(put("/referrals/"+key+"/referred-customer/"+referred),"{\"expectedVersion\":1}"),200);
        call(jwt,body(put("/referrals/"+key+"/referred-customer/"+referred),"{\"expectedVersion\":1}"),409);
        call(jwt,body(post("/referrals/"+key+"/status"),"{\"status\":\"CONTACTED\",\"expectedVersion\":2}"),200);
        call(jwt,body(post("/referrals/"+key+"/status"),"{\"status\":\"CONTACTED\",\"expectedVersion\":2}"),409);
        call(jwt,get("/referrals/summary").param("from","2026-10-20").param("to","2026-10-19"),400);
    }

    @Test void concurrentCreationAndEditionHaveSingleEffect()throws Exception {
        String jwt=http.loginOwner(mockMvc),payload=input(person(jwt),"race@example.test");UUID key=UUID.randomUUID();var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> create=()->mockMvc.perform(body(post("/referrals").header("Idempotency-Key",key),payload).header("Authorization","Bearer "+jwt)).andReturn().getResponse().getStatus();
            var created=pool.invokeAll(java.util.List.of(create,create));assertThat(created.get(0).get()).isEqualTo(201);assertThat(created.get(1).get()).isEqualTo(201);
            String changed=payload.replace("Preferência","Alteração").replace("\"product\"","\"expectedVersion\":1,\"product\"");
            Callable<Integer> update=()->mockMvc.perform(body(patch("/referrals/"+key),changed).header("Authorization","Bearer "+jwt)).andReturn().getResponse().getStatus();
            var edited=pool.invokeAll(java.util.List.of(update,update));assertThat(java.util.List.of(edited.get(0).get(),edited.get(1).get())).containsExactlyInAnyOrder(200,409);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM referral_events WHERE referral_id=? AND action='CREATED'",Long.class,key)).isEqualTo(1);
        }finally {pool.shutdownNow();}
    }

    @Test void deletedReferralPreservesContactPrivacyWithoutCrmAndCustomerHistory()throws Exception {
        String jwt=http.loginOwner(mockMvc),referrer=person(jwt),email="privacy-"+UUID.randomUUID()+"@example.test",payload=input(referrer,email),id=create(jwt,payload);
        call(jwt,delete("/referrals/"+id).param("expectedVersion","1"),204);
        assertThat(call(jwt,delete("/customers/"+referrer),409).path("code").asText()).isEqualTo("REFERRAL_HISTORY_EXISTS");
        String request=call(jwt,body(post("/public/data-deletion-request"),"{\"email\":\""+email+"\"}"),201).path("requestId").asText();
        assertThat(call(jwt,body(post("/agency/data-deletion-requests/"+request+"/process"),"{\"acao\":\"EXCLUIR\"}"),400).path("message").asText()).contains("continua pendente");
        assertThat(jdbc.queryForObject("SELECT status FROM data_deletion_requests WHERE id=?::uuid",String.class,request)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM referral_events WHERE referral_id=?::uuid AND detail->>'participantEmail'=?",Long.class,id,email)).isEqualTo(2);
    }

    @Test void changedReferrerAndLinkedCustomerRemainProtectedAfterDeletion()throws Exception {
        String jwt=http.loginOwner(mockMvc),old=person(jwt),next=person(jwt),referred=person(jwt),payload=input(old,"history@example.test"),id=create(jwt,payload);
        call(jwt,body(patch("/referrals/"+id),payload.replace(old,next).replace("\"product\"","\"expectedVersion\":1,\"product\"")),200);
        call(jwt,body(put("/referrals/"+id+"/referred-customer/"+referred),"{\"expectedVersion\":2}"),200);
        call(jwt,delete("/referrals/"+id).param("expectedVersion","3"),204);
        for(String p:java.util.List.of(old,next,referred)) {
            assertThat(call(jwt,delete("/customers/"+p),409).path("code").asText()).isEqualTo("REFERRAL_HISTORY_EXISTS");
        }
    }

    @Test void contactFromAnotherAgencyDoesNotGrantAccessToPrivacyRequest()throws Exception {
        String jwt=http.loginOwner(mockMvc),email="foreign-referral-"+UUID.randomUUID()+"@example.test";
        Agency other=agencies.save(Agency.builder().name("Indicações isoladas").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.TRIAL).build());
        UUID person=customers.save(CrmCustomer.builder().agency(other).name("Indicador isolado").status(CustomerStatus.ACTIVE).notes("").build()).getId();
        jdbc.update("INSERT INTO referrals(id,agency_id,product,referrer_customer_id,referred_name,referred_email,referred_on,channel,created_by) VALUES(?,?,'MILES',?,'Contato',?,current_date,'EVENT','Teste')",UUID.randomUUID(),other.getId(),person,email);
        String request=call(jwt,body(post("/public/data-deletion-request"),"{\"email\":\""+email+"\"}"),201).path("requestId").asText();
        call(jwt,body(post("/agency/data-deletion-requests/"+request+"/process"),"{\"acao\":\"EXCLUIR\"}"),404);
        assertThat(jdbc.queryForObject("SELECT status FROM data_deletion_requests WHERE id=?::uuid",String.class,request)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM referrals WHERE agency_id=? AND referred_email=?",Long.class,other.getId(),email)).isEqualTo(1);
    }

    @Test void agencyDeletionClearsReferralsAndHistoryButPreservesAnotherAgency() {
        Agency a=agencies.save(Agency.builder().name("Descarte indicações").status(AgencyStatus.DELETION_PENDING).subscriptionStatus(SubscriptionStatus.TRIAL).deletionScheduledAt(Instant.now().minusSeconds(20)).build());
        UUID person=customers.save(CrmCustomer.builder().agency(a).name("Pessoa").status(CustomerStatus.ACTIVE).notes("").build()).getId();
        UUID user=accounts.save(PlatformAccount.builder().agency(a).name("Equipe").email("f09-"+a.getId()+"@example.test").passwordHash("hash").accountKind(AccountKind.AGENCY_OWNER).active(true).build()).getId();
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO referrals(id,agency_id,product,referrer_customer_id,referred_name,referred_email,referred_on,channel,responsible_id,created_by) VALUES(?,?,'MILES',?,'Contato','contact@example.test',current_date,'EVENT',?,'Teste')",id,a.getId(),person,user);
        jdbc.update("INSERT INTO referral_events(id,agency_id,referral_id,action,detail,author) VALUES(?,?,?,'CREATED','{}','Teste')",UUID.randomUUID(),a.getId(),id);
        Agency retained=agencies.save(Agency.builder().name("Preservada indicações").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.TRIAL).build());
        UUID event=UUID.randomUUID();jdbc.update("INSERT INTO referral_events(id,agency_id,referral_id,action,detail,author) VALUES(?,?,?,'DELETED','{}','Teste')",event,retained.getId(),UUID.randomUUID());
        scheduler.executeScheduledDeletions();assertThat(agencies.findById(a.getId())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM referrals WHERE agency_id=?",Long.class,a.getId())).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM referral_events WHERE agency_id=?",Long.class,a.getId())).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM referral_events WHERE id=?",Long.class,event)).isEqualTo(1);
    }
}
