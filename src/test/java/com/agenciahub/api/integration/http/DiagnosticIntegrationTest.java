package com.agenciahub.api.integration.http;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.application.persistence.entity.*;
import com.agenciahub.api.application.persistence.repository.*;
import com.agenciahub.api.domain.*;
import com.agenciahub.api.domain.enums.AccountKind;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DiagnosticIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AgencyRepository agencies;
    @Autowired PlatformAccountRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired DataDeletionRequestRepository deletions;
    @Autowired com.agenciahub.api.scheduling.AgencyDeletionScheduler scheduler;
    @org.springframework.boot.test.mock.mockito.SpyBean com.agenciahub.api.application.services.diagnostics.DiagnosticHistory diagnosticHistory;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    private static final String DATA="""
      {"name":"Pessoa diagnóstico","email":"EMAIL","phone":"+351912345678","profile":{"goals":"Viajar","travelFrequency":"Anual","destinations":"Lisboa","actionPlan":"Revisar"},"items":[{"kind":"CARD","data":{"issuer":"Banco","category":"Black","monthlySpend":3000,"pointsPerDollar":2}},{"kind":"ACCOUNT","data":{"program":"Programa","holder":"Titular declarado","source":"Declaração do cliente","declaredOn":"2026-01-01","openingBalance":1000}},{"kind":"CLUB","data":{"program":"Clube","monthlyCost":50,"monthlyPoints":1000}}]}
      """;
    private String data(){return DATA.replace("EMAIL","diag-"+UUID.randomUUID()+"@test.local");}
    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder r,String value){return r.contentType(MediaType.APPLICATION_JSON).content(value);}
    private JsonNode call(String token,MockHttpServletRequestBuilder r,int code)throws Exception{String s=mockMvc.perform(r.header("Authorization","Bearer "+token)).andExpect(status().is(code)).andReturn().getResponse().getContentAsString();return s.isEmpty()?null:http.parse(s);}
    private String configure(String jwt)throws Exception{var before=call(jwt,get("/miles-diagnostics/config"),200);return call(jwt,body(put("/miles-diagnostics/config"),"{\"title\":\"Diagnóstico\",\"introduction\":\"Envie suas declarações\",\"enabled\":true,\"expectedVersion\":"+(before==null?0:before.path("version").asInt())+"}"),200).path("token").asText();}
    private JsonNode create(String jwt,String data)throws Exception{return call(jwt,body(post("/miles-diagnostics").header("Idempotency-Key",UUID.randomUUID()),data),201);}
    private String person(String jwt)throws Exception{return call(jwt,body(post("/customers"),"{\"name\":\"Pessoa diagnóstico "+UUID.randomUUID()+"\",\"email\":\"diag-person-"+UUID.randomUUID()+"@test.local\",\"status\":\"ACTIVE\"}"),201).path("id").asText();}
    private String apply(int version,String customer,boolean profile,int profileVersion,String items){return "{\"expectedVersion\":"+version+",\"customerId\":\""+customer+"\",\"importProfile\":"+profile+",\"profileVersion\":"+profileVersion+",\"items\":"+items+"}";}
    @Test void publicSubmissionIsPrivateConsentedAndIdempotentWithoutAnyWalletOrCrmWrite()throws Exception{
        String jwt=http.loginOwner(mockMvc),token=configure(jwt),d=data(),key=UUID.randomUUID().toString();
        var counts=new HashMap<String,Long>();for(String table:List.of("customers","customer_miles_profiles","customer_miles_items","customer_miles_movements","sales","trips","financial_entries"))counts.put(table,jdbc.queryForObject("SELECT count(*) FROM "+table,Long.class));
        var config=call("",get("/public/miles-diagnostics/"+token),200);assertThat(config.toString()).contains("consentVersion","agencyName").doesNotContain("email","items");
        String payload="{\"data\":"+d+",\"consent\":true,\"consentVersion\":\"milhas-2026-10-28\"}";
        assertThat(call("",body(post("/public/miles-diagnostics/"+token).header("Idempotency-Key",key),payload),202)).isNull();
        call("",body(post("/public/miles-diagnostics/"+token).header("Idempotency-Key",key),payload),202);
        call("",body(post("/public/miles-diagnostics/"+token).header("Idempotency-Key",key),payload.replace("Lisboa","Porto")),409);
        call("",body(post("/public/miles-diagnostics/"+token).header("Idempotency-Key",UUID.randomUUID()),payload.replace("true","false")),400);
        UUID id=jdbc.queryForObject("SELECT id FROM miles_diagnostics WHERE request_key=?::uuid",UUID.class,key);
        var entry=call(jwt,get("/miles-diagnostics/"+id),200);assertThat(entry.path("source").asText()).isEqualTo("PUBLIC");assertThat(entry.path("consentVersion").asText()).isEqualTo("milhas-2026-10-28");
        call("",get("/miles-diagnostics/"+id),403);call("",get("/public/miles-diagnostics/"+id),404);
        for(var c:counts.entrySet())assertThat(jdbc.queryForObject("SELECT count(*) FROM "+c.getKey(),Long.class)).isEqualTo(c.getValue());
    }
    @Test void fullCrudVersionsArchiveRestoreAndOriginalArePreserved()throws Exception{
        String jwt=http.loginOwner(mockMvc),d=data(),key=UUID.randomUUID().toString();var entry=call(jwt,body(post("/miles-diagnostics").header("Idempotency-Key",key),d),201);String id=entry.path("id").asText();
        assertThat(call(jwt,body(post("/miles-diagnostics").header("Idempotency-Key",key),d),201).path("id")).isEqualTo(entry.path("id"));
        call(jwt,body(post("/miles-diagnostics").header("Idempotency-Key",key),d.replace("Viajar","Mudar")),409);
        entry=call(jwt,body(patch("/miles-diagnostics/"+id),"{\"expectedVersion\":1,\"data\":"+d.replace("Viajar","Planejar")+"}"),200);assertThat(entry.path("id").asText()).isEqualTo(id);assertThat(entry.path("version").asInt()).isEqualTo(2);assertThat(entry.path("original").path("profile").path("goals").asText()).isEqualTo("Viajar");
        call(jwt,body(patch("/miles-diagnostics/"+id),"{\"expectedVersion\":1,\"data\":"+d+"}"),409);
        call(jwt,body(delete("/miles-diagnostics/"+id),"{\"expectedVersion\":2,\"reason\":\"Duplicado\"}"),200);
        call(jwt,body(patch("/miles-diagnostics/"+id),"{\"expectedVersion\":3,\"data\":"+d+"}"),409);
        call(jwt,body(post("/miles-diagnostics/"+id+"/restore"),"{\"expectedVersion\":3,\"reason\":\"Revisar\"}"),200);
        assertThat(call(jwt,get("/miles-diagnostics/"+id),200).path("version").asInt()).isEqualTo(4);assertThat(call(jwt,get("/miles-diagnostics/"+id+"/events"),200).toString()).contains("CREATED","UPDATED","ARCHIVED","RESTORED","Planejar");
    }
    @Test void incorporationIsAtomicReviewedVersionedAndRepeatedConfirmationDoesNotDuplicate()throws Exception{
        String jwt=http.loginOwner(mockMvc),p=person(jwt),d=data(),id=create(jwt,d).path("id").asText(),input=apply(1,p,true,0,"[0,1,2]");
        var pool=Executors.newFixedThreadPool(2);JsonNode receipt;
        try{Callable<JsonNode> action=()->call(jwt,body(post("/miles-diagnostics/"+id+"/incorporate"),input),200);var results=pool.invokeAll(List.of(action,action));receipt=results.get(0).get();assertThat(results.get(1).get()).isEqualTo(receipt);}finally{pool.shutdownNow();}
        var wallet=call(jwt,get("/customers/"+p+"/miles-wallet"),200);assertThat(wallet.path("items").size()).isEqualTo(3);assertThat(wallet.path("movements").size()).isEqualTo(1);assertThat(wallet.path("profile").path("data").path("goals").asText()).isEqualTo("Viajar");
        String user=jdbc.queryForObject("SELECT id::text FROM users WHERE email='admin@agenciahub.com'",String.class);assertThat(receipt.path("authorId").asText()).isEqualTo(user);assertThat(wallet.path("history").get(0).path("author").asText()).isEqualTo(user);
        call(jwt,body(post("/miles-diagnostics/"+id+"/incorporate"),apply(1,p,true,0,"[0]")),409);
        call(jwt,body(patch("/miles-diagnostics/"+id),"{\"expectedVersion\":2,\"data\":"+d+"}"),409);
        call(jwt,body(delete("/miles-diagnostics/"+id),"{\"expectedVersion\":2,\"reason\":\"Concluído\"}"),200);
        assertThat(call(jwt,body(post("/miles-diagnostics/"+id+"/incorporate"),input),200)).isEqualTo(receipt);
        call(jwt,delete("/customers/"+p),409);
    }
    @Test void staleWalletAndInvalidSelectionCannotPartiallyIncorporate()throws Exception{
        String jwt=http.loginOwner(mockMvc),p=person(jwt),d=data(),id=create(jwt,d).path("id").asText();
        call(jwt,body(put("/customers/"+p+"/miles-wallet/profile"),"{\"data\":"+http.parse(d).path("profile")+",\"expectedVersion\":0}"),200);
        call(jwt,body(post("/miles-diagnostics/"+id+"/incorporate"),apply(1,p,true,0,"[0,1]")),409);
        for(String indexes:List.of("[]","[0,0]","[3]"))call(jwt,body(post("/miles-diagnostics/"+id+"/incorporate"),apply(1,p,false,0,indexes)),400);
        assertThat(call(jwt,get("/customers/"+p+"/miles-wallet"),200).path("items")).isEmpty();assertThat(call(jwt,get("/miles-diagnostics/"+id),200).path("receipt").isNull()).isTrue();
        call(jwt,body(post("/miles-diagnostics/"+id+"/incorporate"),apply(1,p,false,0,"[0]")),200);assertThat(call(jwt,get("/customers/"+p+"/miles-wallet"),200).path("items").size()).isEqualTo(1);
    }
    private String member(Agency agency,AccountKind kind)throws Exception{String email="d66-"+UUID.randomUUID()+"@test.local";users.save(PlatformAccount.builder().agency(agency).accountKind(kind).name("Equipe D66").email(email).passwordHash(passwords.encode("test-d66-password")).active(true).emailVerified(true).termsAccepted(true).build());return call("",body(post("/auth/login"),"{\"email\":\""+email+"\",\"password\":\"test-d66-password\"}"),200).path("token").asText();}
    @Test void rolesTenantsAndConfigRevocationAreEnforced()throws Exception{
        String jwt=http.loginOwner(mockMvc),token=configure(jwt),p=person(jwt),id=create(jwt,data()).path("id").asText();Agency own=agencies.findById(jdbc.queryForObject("SELECT agency_id FROM customers WHERE id=?::uuid",UUID.class,p)).orElseThrow();String seller=member(own,AccountKind.SALES_AGENT);
        call(seller,get("/miles-diagnostics/"+id),200);call(seller,delete("/miles-diagnostics/config").param("expectedVersion","1"),403);
        Agency other=agencies.save(Agency.builder().name("Outra D66").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.TRIAL).build());String foreign=member(other,AccountKind.AGENCY_OWNER),foreignPerson=person(foreign);
        call(foreign,get("/miles-diagnostics/"+id),404);call(foreign,get("/miles-diagnostics/"+id+"/events"),404);assertThat(call(foreign,get("/miles-diagnostics"),200)).isEmpty();call(jwt,body(post("/miles-diagnostics/"+id+"/incorporate"),apply(1,foreignPerson,false,0,"[0]")),404);
        String admin=member(null,AccountKind.PLATFORM_ADMIN);call(admin,get("/miles-diagnostics"),403);
        var config=call(jwt,get("/miles-diagnostics/config"),200);call(jwt,delete("/miles-diagnostics/config").param("expectedVersion",config.path("version").asText()),200);call("",get("/public/miles-diagnostics/"+token),404);call("",body(post("/public/miles-diagnostics/"+token).header("Idempotency-Key",UUID.randomUUID()),"{\"data\":"+data()+",\"consent\":true,\"consentVersion\":\"milhas-2026-10-28\"}"),404);
        assertThat(configure(jwt)).isNotEqualTo(token);
    }
    @Test void failureMidBatchRollsBackProfileItemsReceiptAndHistory()throws Exception{
        String jwt=http.loginOwner(mockMvc),p=person(jwt),id=create(jwt,data()).path("id").asText();
        jdbc.execute("CREATE FUNCTION d66_reject_item() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.kind='ACCOUNT' THEN RAISE EXCEPTION 'falha teste D66'; END IF; RETURN NEW; END $$");jdbc.execute("CREATE TRIGGER d66_failure BEFORE INSERT ON customer_miles_items FOR EACH ROW EXECUTE FUNCTION d66_reject_item()");
        try{call(jwt,body(post("/miles-diagnostics/"+id+"/incorporate"),apply(1,p,true,0,"[0,1]")),500);}finally{jdbc.execute("DROP TRIGGER d66_failure ON customer_miles_items");jdbc.execute("DROP FUNCTION d66_reject_item()");}
        var wallet=call(jwt,get("/customers/"+p+"/miles-wallet"),200);assertThat(wallet.path("profile").isNull()).isTrue();assertThat(wallet.path("items")).isEmpty();assertThat(wallet.path("history")).isEmpty();assertThat(call(jwt,get("/miles-diagnostics/"+id),200).path("receipt").isNull()).isTrue();
    }
    @Test void privacyFindsOriginalAndIntermediateEmailsWithoutCustomerAndKeepsRequestPending()throws Exception{
        String jwt=http.loginOwner(mockMvc),d=data(),id=create(jwt,d).path("id").asText(),original=http.parse(d).path("email").asText(),middle="middle-"+UUID.randomUUID()+"@test.local";
        call(jwt,body(patch("/miles-diagnostics/"+id),"{\"expectedVersion\":1,\"data\":"+d.replace(original,middle)+"}"),200);call(jwt,body(patch("/miles-diagnostics/"+id),"{\"expectedVersion\":2,\"data\":"+d.replace(original,"latest@test.local")+"}"),200);
        for(String email:List.of(original,middle)){var req=call("",body(post("/public/data-deletion-request"),"{\"email\":\""+email+"\"}"),201);call(jwt,body(post("/agency/data-deletion-requests/"+req.path("requestId").asText()+"/process"),"{\"acao\":\"EXCLUIR\"}"),400);assertThat(deletions.findById(UUID.fromString(req.path("requestId").asText())).orElseThrow().getStatus()).isEqualTo(DataDeletionStatus.PENDING);}
    }
    @Test void newWalletAuthorsUseStableSessionIdAndDoNotRewriteLegacyAuthors()throws Exception{
        String jwt=http.loginOwner(mockMvc),p=person(jwt),base="/customers/"+p+"/miles-wallet",key=UUID.randomUUID().toString();String card="{\"kind\":\"CARD\",\"data\":{\"issuer\":\"Banco\",\"category\":\"Cartão\"}}";
        call(jwt,body(post(base+"/items").header("Idempotency-Key",key),card),201);jdbc.update("UPDATE customer_miles_audit SET author='autor legado' WHERE resource_id=?::uuid",key);
        call(jwt,body(patch(base+"/items/"+key),card.replace("Banco","Emissor alterado").replace("}}","},\"expectedVersion\":1}")),200);
        String user=jdbc.queryForObject("SELECT id::text FROM users WHERE email='admin@agenciahub.com'",String.class);var history=call(jwt,get(base),200).path("history");assertThat(history.toString()).contains("autor legado",user);
    }
    @Test void quotaConsentAndValidationProtectPublicSubmissions()throws Exception{
        Agency agency=agencies.save(Agency.builder().name("Quota D66").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.TRIAL).build());String jwt=member(agency,AccountKind.AGENCY_OWNER),token=configure(jwt),d=data();
        for(String payload:List.of("{\"data\":"+d+",\"consent\":true}","{\"data\":"+d+",\"consent\":true,\"consentVersion\":\"antiga\"}","{\"data\":"+d.replace("\"monthlySpend\":3000","\"monthlySpend\":-1")+",\"consent\":true,\"consentVersion\":\"milhas-2026-10-28\"}"))call("",body(post("/public/miles-diagnostics/"+token).header("Idempotency-Key",UUID.randomUUID()),payload),400);
        call(jwt,body(post("/miles-diagnostics").header("Idempotency-Key",UUID.randomUUID()),d.replace("\"CARD\"","\"EXPIRY\"")),400);
        jdbc.update("INSERT INTO miles_diagnostics(id,agency_id,request_key,original_data,data,source,consent_version) SELECT gen_random_uuid(),?,gen_random_uuid(),?::jsonb,?::jsonb,'PUBLIC','milhas-2026-10-28' FROM generate_series(1,60)",agency.getId(),d,d);
        String payload="{\"data\":"+d+",\"consent\":true,\"consentVersion\":\"milhas-2026-10-28\"}";
        call("",body(post("/public/miles-diagnostics/"+token).header("Idempotency-Key",UUID.randomUUID()),payload),429);
        jdbc.update("UPDATE miles_diagnostics SET created_at=now()-interval '2 hours' WHERE agency_id=?",agency.getId());call("",body(post("/public/miles-diagnostics/"+token).header("Idempotency-Key",UUID.randomUUID()),payload),202);
    }
    @Test void agencyHardDeleteRemovesDiagnosticsAndWalletMovementsWithoutAffectingAnotherAgency()throws Exception{
        Agency agency=agencies.save(Agency.builder().name("Descarte D66").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.TRIAL).build());String jwt=member(agency,AccountKind.AGENCY_OWNER),p=person(jwt),id=create(jwt,data()).path("id").asText();configure(jwt);
        call(jwt,body(post("/miles-diagnostics/"+id+"/incorporate"),apply(1,p,true,0,"[0,1,2]")),200);
        String other=http.loginOwner(mockMvc),otherId=create(other,data()).path("id").asText();
        agency.setStatus(AgencyStatus.DELETION_PENDING);agency.setDeletionScheduledAt(java.time.Instant.now().minusSeconds(60));agencies.save(agency);scheduler.executeScheduledDeletions();
        assertThat(agencies.findById(agency.getId())).isEmpty();for(String table:List.of("miles_diagnostics","miles_diagnostic_configs","miles_diagnostic_events","customer_miles_items","customer_miles_profiles","customer_miles_audit"))assertThat(jdbc.queryForObject("SELECT count(*) FROM "+table+" WHERE agency_id=?",Long.class,agency.getId())).isZero();
        assertThat(call(other,get("/miles-diagnostics/"+otherId),200).path("id").asText()).isEqualTo(otherId);
    }
    @Test void privacyRechecksDiagnosticAfterCustomerLockWhenIncorporationCommitsConcurrently()throws Exception{
        String jwt=http.loginOwner(mockMvc),p=person(jwt),id=create(jwt,data()).path("id").asText();String email=jdbc.queryForObject("SELECT email FROM customers WHERE id=?::uuid",String.class,p);
        var request=call("",body(post("/public/data-deletion-request"),"{\"email\":\""+email+"\"}"),201);String requestId=request.path("requestId").asText();
        var uncommitted=new CountDownLatch(1);var checked=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        org.mockito.Mockito.doAnswer(invocation->{boolean found=(boolean)invocation.callRealMethod();if(invocation.getArgument(1).equals(UUID.fromString(p))&&!found)checked.countDown();return found;}).when(diagnosticHistory).customer(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
        try{
            var incorporation=pool.submit(()->new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status->{try{call(jwt,body(post("/miles-diagnostics/"+id+"/incorporate"),apply(1,p,false,0,"[0]")),200);uncommitted.countDown();if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException("timeout de teste");}catch(Exception e){throw new RuntimeException(e);}}));
            assertThat(uncommitted.await(10,TimeUnit.SECONDS)).isTrue();
            var privacy=pool.submit(()->call(jwt,body(post("/agency/data-deletion-requests/"+requestId+"/process"),"{\"acao\":\"EXCLUIR\"}"),400));
            assertThat(checked.await(10,TimeUnit.SECONDS)).isTrue();release.countDown();incorporation.get(10,TimeUnit.SECONDS);assertThat(privacy.get(10,TimeUnit.SECONDS).path("message").asText()).contains("diagnóstico","pendente");
            assertThat(deletions.findById(UUID.fromString(requestId)).orElseThrow().getStatus()).isEqualTo(DataDeletionStatus.PENDING);assertThat(jdbc.queryForObject("SELECT name FROM customers WHERE id=?::uuid",String.class,p)).doesNotContain("EXCLUÍDO");
        }finally{release.countDown();pool.shutdownNow();org.mockito.Mockito.reset(diagnosticHistory);}
    }
}
