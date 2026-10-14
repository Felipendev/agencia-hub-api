package com.agenciahub.api.integration.http;

import com.agenciahub.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

class MilesWalletDirectoryIntegrationTest extends AbstractIntegrationTest {
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired com.agenciahub.api.application.persistence.repository.AgencyRepository agencies;
    @Autowired com.agenciahub.api.application.persistence.repository.CrmCustomerRepository customers;
    private String customer(String token) throws Exception {
        String response=mockMvc.perform(post("/customers").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Pessoa de dois produtos %s\",\"status\":\"ACTIVE\"}".formatted(UUID.randomUUID())))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return http.parse(response).path("id").asText();
    }
    private void createItem(String jwt,String id,UUID key,String kind,String data) throws Exception {
        mockMvc.perform(post("/customers/"+id+"/miles-wallet/items").header("Authorization","Bearer "+jwt).header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON)
            .content("{\"kind\":\"%s\",\"data\":%s}".formatted(kind,data))).andExpect(status().isCreated());
    }
    @Test void milesRelationshipUsesSamePersonAndDoesNotEnrollEveryTravelCustomer() throws Exception {
        String jwt=http.loginOwner(mockMvc),id=customer(jwt);
        int peopleBefore=jdbc.queryForObject("SELECT count(*) FROM customers",Integer.class);
        mockMvc.perform(get("/miles-wallets").header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.customerId == '%s')]".formatted(id)).isEmpty());
        mockMvc.perform(put("/customers/"+id+"/miles-wallet/profile").header("Authorization","Bearer "+jwt).contentType(MediaType.APPLICATION_JSON).content("{\"data\":{\"goals\":\"Acumular pontos\"},\"expectedVersion\":0}")).andExpect(status().isOk());
        mockMvc.perform(get("/miles-wallets").header("Authorization","Bearer "+jwt)).andExpect(jsonPath("$[?(@.customerId == '%s')].archived".formatted(id)).value(false));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customers",Integer.class)).isEqualTo(peopleBefore);
        mockMvc.perform(delete("/customers/"+id+"/miles-wallet/profile?expectedVersion=1").header("Authorization","Bearer "+jwt)).andExpect(status().isNoContent());
        mockMvc.perform(get("/miles-wallets").header("Authorization","Bearer "+jwt)).andExpect(jsonPath("$[?(@.customerId == '%s')].archived".formatted(id)).value(true));
        mockMvc.perform(get("/customers/"+id).header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
    }
    @Test void countsAndExpiryFollowMileageDataAndAccountArchiving() throws Exception {
        String jwt=http.loginOwner(mockMvc),id=customer(jwt);UUID account=UUID.randomUUID();
        createItem(jwt,id,account,"ACCOUNT","{\"program\":\"Programa\",\"holder\":\"Titular\",\"source\":\"Extrato\",\"openingBalance\":1000,\"declaredOn\":\"2026-01-01\"}");
        createItem(jwt,id,UUID.randomUUID(),"CARD","{\"issuer\":\"Emissor\",\"category\":\"Categoria\"}");
        createItem(jwt,id,UUID.randomUUID(),"CLUB","{\"program\":\"Clube\",\"monthlyCost\":50,\"monthlyPoints\":1000}");
        String expiry=com.agenciahub.api.domain.MilesWallet.today().plusDays(20).toString();
        createItem(jwt,id,UUID.randomUUID(),"EXPIRY","{\"accountId\":\"%s\",\"quantity\":100,\"expiresOn\":\"%s\"}".formatted(account,expiry));
        mockMvc.perform(get("/miles-wallets").header("Authorization","Bearer "+jwt)).andExpect(jsonPath("$[?(@.customerId == '%s')].accounts".formatted(id)).value(1)).andExpect(jsonPath("$[?(@.customerId == '%s')].cards".formatted(id)).value(1)).andExpect(jsonPath("$[?(@.customerId == '%s')].clubs".formatted(id)).value(1)).andExpect(jsonPath("$[?(@.customerId == '%s')].nextExpiry".formatted(id)).value(expiry));
        mockMvc.perform(delete("/customers/"+id+"/miles-wallet/items/"+account+"?expectedVersion=1").header("Authorization","Bearer "+jwt)).andExpect(status().isNoContent());
        mockMvc.perform(get("/miles-wallets").header("Authorization","Bearer "+jwt)).andExpect(jsonPath("$[?(@.customerId == '%s')].accounts".formatted(id)).value(0)).andExpect(jsonPath("$[?(@.customerId == '%s')].nextExpiry".formatted(id)).value(org.hamcrest.Matchers.contains((Object)null)));
    }
    @Test void directoryIsAgencyScopedAndRequiresAuthentication() throws Exception {
        String jwt=http.loginOwner(mockMvc);
        var other=agencies.save(com.agenciahub.api.application.persistence.entity.Agency.builder().name("Milhas privadas").status(com.agenciahub.api.domain.AgencyStatus.ACTIVE).subscriptionStatus(com.agenciahub.api.domain.SubscriptionStatus.ACTIVE).build());
        var person=customers.save(com.agenciahub.api.application.persistence.entity.CrmCustomer.builder().agency(other).name("Pessoa privada").status(com.agenciahub.api.domain.CustomerStatus.ACTIVE).build());
        jdbc.update("INSERT INTO customer_miles_profiles(customer_id,agency_id,data) VALUES(?,?,?::jsonb)",person.getId(),other.getId(),"{\"goals\":\"Privada\"}");
        mockMvc.perform(get("/miles-wallets").header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.customerId == '%s')]".formatted(person.getId())).isEmpty());
        mockMvc.perform(get("/miles-wallets")).andExpect(com.agenciahub.api.support.IntegrationHttpSupport.unauthenticated());
    }
}
