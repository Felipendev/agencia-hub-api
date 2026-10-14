package com.agenciahub.api.integration.http;

import com.agenciahub.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

class MilesWalletIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired com.agenciahub.api.application.persistence.repository.AgencyRepository agencies;
    @Autowired com.agenciahub.api.application.persistence.repository.CrmCustomerRepository customers;
    private static final String ACCOUNT="""
      {"kind":"ACCOUNT","data":{"program":"Smiles","holder":"Titular declarado","source":"Extrato manual","declaredOn":"2026-01-01","openingBalance":1000}}
      """;
    private String customer(String jwt) throws Exception {
        String response=mockMvc.perform(post("/customers").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Carteira de teste %s\",\"status\":\"ACTIVE\"}".formatted(UUID.randomUUID())))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return "/customers/"+http.parse(response).path("id").asText()+"/miles-wallet";
    }
    private String create(String jwt,String base,String body,UUID key) throws Exception {
        return mockMvc.perform(post(base+"/items").header("Authorization","Bearer "+jwt).header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }
    @Test void declaredBalanceIsImmutableAndMovementsAreIdempotentAndReversible() throws Exception {
        String jwt=http.loginOwner(mockMvc),base=customer(jwt);UUID account=UUID.randomUUID();
        create(jwt,base,ACCOUNT,account);create(jwt,base,ACCOUNT,account);
        String key=UUID.randomUUID().toString();String movement="{\"requestKey\":\"%s\",\"quantity\":-200,\"reason\":\"Emissão manual\",\"occurredOn\":\"2026-01-02\"}".formatted(key);
        String result=mockMvc.perform(post(base+"/accounts/"+account+"/movements").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(movement)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mockMvc.perform(post(base+"/accounts/"+account+"/movements").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(movement)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(http.parse(result).path("id").asText()));
        mockMvc.perform(get(base).header("Authorization","Bearer "+jwt)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].balance").value(800)).andExpect(jsonPath("$.movements.length()").value(2));
        mockMvc.perform(patch(base+"/items/"+account).header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(ACCOUNT.replace("1000","9999"))).andExpect(status().isBadRequest());
        String reverse=base+"/movements/"+http.parse(result).path("id").asText()+"/reverse";
        for(int i=0;i<2;i++)mockMvc.perform(post(reverse).header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Correção\"}")).andExpect(status().isOk());
        mockMvc.perform(get(base).header("Authorization","Bearer "+jwt)).andExpect(jsonPath("$.items[0].balance").value(1000)).andExpect(jsonPath("$.movements.length()").value(3));
        mockMvc.perform(delete(base+"/items/"+account+"?expectedVersion=1").header("Authorization","Bearer "+jwt)).andExpect(status().isNoContent());
        mockMvc.perform(get(base).header("Authorization","Bearer "+jwt)).andExpect(jsonPath("$.items[0].archived").value(true)).andExpect(jsonPath("$.items[0].balance").value(1000));
        mockMvc.perform(post(base+"/items/"+account+"/restore?expectedVersion=2").header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$.archived").value(false));
    }
    @Test void profilesCardsClubsAndExpiriesHavePersistentCrudAndAudit() throws Exception {
        String jwt=http.loginOwner(mockMvc),base=customer(jwt);UUID account=UUID.randomUUID();create(jwt,base,ACCOUNT,account);
        String profile="{\"data\":{\"goals\":\"Viajar\",\"travelFrequency\":\"Anual\",\"destinations\":\"Lisboa\",\"actionPlan\":\"Revisar clubes\"},\"expectedVersion\":0}";
        mockMvc.perform(put(base+"/profile").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(profile)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mockMvc.perform(put(base+"/profile").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(profile.replace("Viajar","Planejar").replace("expectedVersion\":0","expectedVersion\":1"))).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
        mockMvc.perform(put(base+"/profile").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(profile)).andExpect(status().isConflict());
        for(String body:new String[]{"{\"kind\":\"CARD\",\"data\":{\"issuer\":\"Banco\",\"category\":\"Categoria\"}}","{\"kind\":\"CLUB\",\"data\":{\"program\":\"Smiles\",\"monthlyCost\":50,\"monthlyPoints\":1000}}","{\"kind\":\"EXPIRY\",\"data\":{\"accountId\":\"%s\",\"quantity\":100,\"expiresOn\":\"2026-12-01\"}}".formatted(account)}){
            UUID id=UUID.randomUUID();create(jwt,base,body,id);
            String changed=body.replace("Banco","Emissor alterado").replace("monthlyCost\":50","monthlyCost\":60").replace("quantity\":100","quantity\":200");
            changed=changed.substring(0,changed.length()-1)+",\"expectedVersion\":1}";
            mockMvc.perform(patch(base+"/items/"+id).header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(changed)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString())).andExpect(jsonPath("$.version").value(2));
            mockMvc.perform(delete(base+"/items/"+id+"?expectedVersion=2").header("Authorization","Bearer "+jwt)).andExpect(status().isNoContent());
            mockMvc.perform(post(base+"/items").header("Authorization","Bearer "+jwt).header("Idempotency-Key",id).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        }
        mockMvc.perform(delete(base+"/profile?expectedVersion=2").header("Authorization","Bearer "+jwt)).andExpect(status().isNoContent());
        mockMvc.perform(get(base).header("Authorization","Bearer "+jwt)).andExpect(jsonPath("$.profile.archived").value(true)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.history.length()").value(13));
    }
    @Test void concurrentDebitsCannotExceedTheDeclaredBalance() throws Exception {
        String jwt=http.loginOwner(mockMvc),base=customer(jwt);UUID account=UUID.randomUUID();create(jwt,base,ACCOUNT,account);
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        try{
            java.util.List<java.util.concurrent.Callable<Integer>> calls=new java.util.ArrayList<>();
            for(int i=0;i<2;i++){String body="{\"requestKey\":\"%s\",\"quantity\":-700,\"reason\":\"Saída\",\"occurredOn\":\"2026-01-02\"}".formatted(UUID.randomUUID());calls.add(()->mockMvc.perform(post(base+"/accounts/"+account+"/movements").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus());}
            var results=executor.invokeAll(calls);assertThat(java.util.List.of(results.get(0).get(),results.get(1).get())).containsExactlyInAnyOrder(201,409);
        }finally{executor.shutdownNow();}
        mockMvc.perform(get(base).header("Authorization","Bearer "+jwt)).andExpect(jsonPath("$.items[0].balance").value(300));
    }
    @Test void agencyScopeProtectsEveryOperationAndExpiryReferences() throws Exception {
        String jwt=http.loginOwner(mockMvc),own=customer(jwt);UUID account=UUID.randomUUID();create(jwt,own,ACCOUNT,account);
        var other=agencies.save(com.agenciahub.api.application.persistence.entity.Agency.builder().name("Carteira isolada").status(com.agenciahub.api.domain.AgencyStatus.ACTIVE).subscriptionStatus(com.agenciahub.api.domain.SubscriptionStatus.ACTIVE).build());
        var person=customers.save(com.agenciahub.api.application.persistence.entity.CrmCustomer.builder().agency(other).name("Pessoa isolada").status(com.agenciahub.api.domain.CustomerStatus.ACTIVE).build());
        String base="/customers/"+person.getId()+"/miles-wallet";
        mockMvc.perform(get(base).header("Authorization","Bearer "+jwt)).andExpect(status().isNotFound());
        mockMvc.perform(put(base+"/profile").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content("{\"data\":{},\"expectedVersion\":0}")).andExpect(status().isNotFound());
        mockMvc.perform(post(base+"/items").header("Authorization","Bearer "+jwt).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(ACCOUNT)).andExpect(status().isNotFound());
        mockMvc.perform(delete(base+"/items/"+account+"?expectedVersion=1").header("Authorization","Bearer "+jwt)).andExpect(status().isNotFound());
        mockMvc.perform(patch(base+"/items/"+account).header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content(ACCOUNT)).andExpect(status().isNotFound());
        String another=customer(jwt);
        mockMvc.perform(post(another+"/items").header("Authorization","Bearer "+jwt).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content("{\"kind\":\"EXPIRY\",\"data\":{\"accountId\":\"%s\",\"quantity\":1,\"expiresOn\":\"2026-12-01\"}}".formatted(account))).andExpect(status().isNotFound());
        mockMvc.perform(get(own)).andExpect(com.agenciahub.api.support.IntegrationHttpSupport.unauthenticated());
    }
    @Test void invalidQuantitiesAndFutureDeclaredBalancesAreRejected() throws Exception {
        String jwt=http.loginOwner(mockMvc),base=customer(jwt);
        for(String body:new String[]{ACCOUNT.replace("1000","1.5"),ACCOUNT.replace("1000","-1"),ACCOUNT.replace("2026-01-01","2099-01-01"),ACCOUNT.replace("Extrato manual","")})
            mockMvc.perform(post(base+"/items").header("Authorization","Bearer "+jwt).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mockMvc.perform(get(base).header("Authorization","Bearer "+jwt)).andExpect(jsonPath("$.items.length()").value(0));
    }
}
