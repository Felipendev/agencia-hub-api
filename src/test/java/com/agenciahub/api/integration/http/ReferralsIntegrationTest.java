package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.CustomerStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** F09 — indicações com banco embarcado e Flyway. Sem créditos ou efeitos financeiros. */
class ReferralsIntegrationTest extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired AgencyRepository agencies;
    @Autowired CrmCustomerRepository customers;
    @Autowired PlatformAccountRepository accounts;
    @Autowired PasswordEncoder passwords;

    private static final String TODAY = LocalDate.now(ZoneId.of("America/Fortaleza")).toString();
    private static final String YESTERDAY = LocalDate.now(ZoneId.of("America/Fortaleza")).minusDays(1).toString();

    private MockHttpServletRequestBuilder as(String jwt, MockHttpServletRequestBuilder b) { return b.header("Authorization", "Bearer " + jwt); }

    private JsonNode json(String jwt, MockHttpServletRequestBuilder b, int expected) throws Exception {
        String body = mockMvc.perform(as(jwt, b)).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return body.isEmpty() ? null : http.parse(body);
    }

    private static MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder b, String json) {
        return b.contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private String person(String jwt, String phone) throws Exception {
        String extra = phone == null ? "" : ",\"phone\":\"" + phone + "\"";
        return json(jwt, body(post("/customers"), "{\"name\":\"Indicação %s\",\"status\":\"ACTIVE\"%s}".formatted(UUID.randomUUID(), extra)), 201).path("id").asText();
    }

    private static String lead(String product, String referrer, String extra) {
        return ("{\"product\":\"%s\",\"referrerId\":\"%s\",\"referredName\":\"Bia Souza\",\"referredPhone\":\"(85) 98888-%s\","
                + "\"referredOn\":\"%s\",\"channel\":\"WHATSAPP\"%s}").formatted(product, referrer, String.valueOf(1000 + (int) (Math.random() * 8999)), YESTERDAY, extra);
    }

    private static String crm(String product, String referrer, String referred, String extra) {
        return "{\"product\":\"%s\",\"referrerId\":\"%s\",\"referredCustomerId\":\"%s\",\"referredOn\":\"%s\",\"channel\":\"EVENT\",\"channelDetail\":\"Feira\"%s}"
                .formatted(product, referrer, referred, YESTERDAY, extra);
    }

    private JsonNode create(String jwt, String payload, int expected) throws Exception {
        return json(jwt, body(post("/referrals").header("Idempotency-Key", UUID.randomUUID()), payload), expected);
    }

    private long count(String table) { return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class); }

    private String member(AccountKind kind, Agency agency) throws Exception {
        String email = "indica-" + UUID.randomUUID() + "@test.local";
        accounts.save(PlatformAccount.builder().agency(agency).name("Equipe indicações").email(email)
                .passwordHash(passwords.encode("indica-test-password")).accountKind(kind)
                .active(true).emailVerified(true).termsAccepted(true).build());
        String login = mockMvc.perform(body(post("/auth/login"), "{\"email\":\"%s\",\"password\":\"indica-test-password\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return http.parse(login).path("token").asText();
    }

    @Test
    void crudIsIdempotentKeepsIdAuditsAndHasNoFinancialEffects() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String ana = person(jwt, null), caio = person(jwt, null);
        long sales = count("sales"), entries = count("financial_entries"), coupons = count("coupons"), people = count("customers");
        UUID key = UUID.randomUUID();
        String payload = lead("TRAVEL", ana, "");
        JsonNode created = json(jwt, body(post("/referrals").header("Idempotency-Key", key), payload), 201);
        JsonNode again = json(jwt, body(post("/referrals").header("Idempotency-Key", key), payload), 201);
        String id = created.path("referral").path("id").asText();
        assertThat(id).isEqualTo(key.toString()).isEqualTo(again.path("referral").path("id").asText());
        assertThat(created.path("referral").path("status").asText()).isEqualTo("RECEIVED");
        assertThat(created.path("referral").path("referredPhone").asText()).startsWith("85988");
        json(jwt, body(post("/referrals").header("Idempotency-Key", key), payload.replace("Bia Souza", "Outra")), 409);
        create(jwt, payload.replace(YESTERDAY, LocalDate.parse(TODAY).plusDays(1).toString()), 400);
        create(jwt, payload.replace("\"product\":\"TRAVEL\",", ""), 400);
        create(jwt, payload.replace("\"WHATSAPP\"", "\"OTHER\""), 400);

        // Edição pelo mesmo ID, com versão; produto não muda.
        String edit = payload.replace("\"referrerId\":\"" + ana, "\"referrerId\":\"" + caio).replace("}", ",\"notes\":\"Prefere à noite\",\"expectedVersion\":1}");
        JsonNode updated = json(jwt, body(patch("/referrals/" + id), edit), 200);
        assertThat(updated.path("referral").path("id").asText()).isEqualTo(id);
        assertThat(updated.path("referral").path("version").asInt()).isEqualTo(2);
        assertThat(updated.path("history").get(0).path("action").asText()).isEqualTo("UPDATED");
        assertThat(updated.path("history").get(0).path("detail").path("referrerId").path("from").asText()).isEqualTo(ana);
        assertThat(updated.path("history").get(0).path("detail").toString()).contains("notesChanged").doesNotContain("Prefere à noite");
        json(jwt, body(patch("/referrals/" + id), edit), 409);
        json(jwt, body(patch("/referrals/" + id), edit.replace("\"TRAVEL\"", "\"MILES\"").replace("\"expectedVersion\":1", "\"expectedVersion\":2")), 400);

        // Persistência após recarregar, filtros e exclusão auditada; chave excluída não recria.
        assertThat(json(jwt, get("/referrals/" + id), 200).path("referral").path("referrerId").asText()).isEqualTo(caio);
        assertThat(json(jwt, get("/referrals").param("customerId", caio).param("product", "TRAVEL"), 200).toString()).contains(id);
        assertThat(json(jwt, get("/referrals").param("customerId", caio).param("product", "MILES"), 200).toString()).doesNotContain(id);
        assertThat(json(jwt, get("/referrals").param("q", "bia souza"), 200).toString()).contains(id);
        json(jwt, get("/referrals").param("from", TODAY).param("to", YESTERDAY), 400);
        json(jwt, delete("/referrals/" + id).param("expectedVersion", "2"), 204);
        json(jwt, get("/referrals/" + id), 404);
        json(jwt, body(post("/referrals").header("Idempotency-Key", key), payload), 409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM referral_events WHERE referral_id = ?::uuid AND action = 'DELETED'", Long.class, id)).isEqualTo(1);

        assertThat(count("sales")).isEqualTo(sales);
        assertThat(count("financial_entries")).isEqualTo(entries);
        assertThat(count("coupons")).isEqualTo(coupons);
        assertThat(count("customers")).isEqualTo(people);
    }

    @Test
    void onePersonHasOneActiveReferralPerProduct() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String ana = person(jwt, null), davi = person(jwt, null), bia = person(jwt, null);
        String first = create(jwt, crm("TRAVEL", ana, bia, ""), 201).path("referral").path("id").asText();
        JsonNode duplicate = create(jwt, crm("TRAVEL", davi, bia, ""), 409);
        assertThat(duplicate.path("message").asText()).contains("já tem uma indicação ativa em Viagens");
        create(jwt, crm("MILES", davi, bia, ""), 201);
        create(jwt, crm("TRAVEL", ana, ana, ""), 400);

        json(jwt, body(post("/referrals/" + first + "/status"), "{\"status\":\"CANCELLED\",\"reason\":\"Registrada para a pessoa errada\",\"expectedVersion\":1}"), 200);
        String second = create(jwt, crm("TRAVEL", davi, bia, ""), 201).path("referral").path("id").asText();
        // Reativar a primeira esbarraria na segunda, ativa.
        json(jwt, body(post("/referrals/" + first + "/status"), "{\"status\":\"RECEIVED\",\"reason\":\"Engano\",\"expectedVersion\":2}"), 409);
        json(jwt, body(patch("/referrals/" + first), crm("TRAVEL", ana, bia, ",\"expectedVersion\":2")), 409);
        assertThat(json(jwt, get("/referrals/" + second), 200).path("referral").path("status").asText()).isEqualTo("RECEIVED");
    }

    @Test
    void lifecycleLinkingConversionAndWarnings() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String ana = person(jwt, null), bia = person(jwt, "+55 85 97777-4321"), other = person(jwt, null);
        UUID agency = jdbc.queryForObject("SELECT agency_id FROM customers WHERE id = ?::uuid", UUID.class, ana);
        String payload = "{\"product\":\"TRAVEL\",\"referrerId\":\"%s\",\"referredName\":\"Bia\",\"referredPhone\":\"(85) 97777-4321\",\"referredOn\":\"%s\",\"channel\":\"CONVERSATION\"}"
                .formatted(ana, YESTERDAY);
        JsonNode created = create(jwt, payload, 201);
        String id = created.path("referral").path("id").asText();
        assertThat(created.path("warnings").get(0).path("kind").asText()).isEqualTo("CUSTOMER");
        assertThat(created.path("warnings").get(0).path("id").asText()).isEqualTo(bia);

        json(jwt, body(post("/referrals/" + id + "/status"), "{\"status\":\"CONTACTED\",\"expectedVersion\":1}"), 200);
        json(jwt, body(post("/referrals/" + id + "/status"), "{\"status\":\"CONVERTED\",\"convertedOn\":\"%s\",\"expectedVersion\":2}".formatted(TODAY)), 400);

        json(jwt, body(put("/referrals/" + id + "/referred-customer/" + ana), "{\"expectedVersion\":2}"), 400);
        JsonNode linked = json(jwt, body(put("/referrals/" + id + "/referred-customer/" + bia), "{\"expectedVersion\":2}"), 200);
        assertThat(linked.path("referral").path("referredCustomerId").asText()).isEqualTo(bia);
        assertThat(linked.path("referral").path("referredPhone").isNull()).isTrue();
        assertThat(linked.path("warnings").size()).isZero();
        assertThat(linked.path("history").get(0).path("detail").path("previousPhone").asText()).isEqualTo("85977774321");
        json(jwt, get("/customers/" + bia), 200);

        UUID ownSale = UUID.randomUUID(), foreignSale = UUID.randomUUID();
        jdbc.update("INSERT INTO sales(id, agency_id, customer_id, total_amount, sale_date, status) VALUES (?, ?, ?::uuid, 1000, current_date, 'OPEN')", ownSale, agency, bia);
        jdbc.update("INSERT INTO sales(id, agency_id, customer_id, total_amount, sale_date, status) VALUES (?, ?, ?::uuid, 1000, current_date, 'OPEN')", foreignSale, agency, other);
        String convert = "{\"status\":\"CONVERTED\",\"convertedOn\":\"%s\",\"saleId\":\"%s\",\"expectedVersion\":3}";
        json(jwt, body(post("/referrals/" + id + "/status"), convert.formatted(TODAY, foreignSale)), 404);
        json(jwt, body(post("/referrals/" + id + "/status"), convert.formatted(LocalDate.parse(YESTERDAY).minusDays(5), ownSale)), 400);
        JsonNode converted = json(jwt, body(post("/referrals/" + id + "/status"), convert.formatted(TODAY, ownSale)), 200);
        assertThat(converted.path("referral").path("everConverted").asBoolean()).isTrue();
        assertThat(converted.path("referral").path("saleId").asText()).isEqualTo(ownSale.toString());

        json(jwt, delete("/referrals/" + id).param("expectedVersion", "4"), 409);
        json(jwt, body(post("/referrals/" + id + "/status"), "{\"status\":\"NOT_CONVERTED\",\"reason\":\"x\",\"expectedVersion\":4}"), 400);
        JsonNode reopened = json(jwt, body(post("/referrals/" + id + "/status"), "{\"status\":\"CONTACTED\",\"reason\":\"Venda cancelada pelo cliente\",\"expectedVersion\":4}"), 200);
        assertThat(reopened.path("referral").path("saleId").isNull()).isTrue();
        assertThat(reopened.path("history").get(0).path("detail").path("previousSaleId").asText()).isEqualTo(ownSale.toString());
        json(jwt, delete("/referrals/" + id).param("expectedVersion", "5"), 409);
        JsonNode cancelled = json(jwt, body(post("/referrals/" + id + "/status"), "{\"status\":\"CANCELLED\",\"reason\":\"Encerrada\",\"expectedVersion\":5}"), 200);
        assertThat(cancelled.path("history").toString()).contains("CREATED", "STATUS_CONTACTED", "REFERRED_LINKED", "STATUS_CONVERTED", "STATUS_CANCELLED").doesNotContain("password");

        // Milhas não aceita venda.
        String miles = create(jwt, crm("MILES", ana, bia, ""), 201).path("referral").path("id").asText();
        json(jwt, body(post("/referrals/" + miles + "/status"), convert.formatted(TODAY, ownSale).replace("\"expectedVersion\":3", "\"expectedVersion\":1")), 400);

        JsonNode summary = json(jwt, get("/referrals/summary").param("product", "TRAVEL"), 200);
        assertThat(summary.path("counts").path("CANCELLED").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(summary.toString()).doesNotContain("amount");
    }

    @Test
    void agencyScopeAndRolesProtectReferrals() throws Exception {
        String owner = http.loginOwner(mockMvc);
        String ana = person(owner, null);
        Agency agency = customers.findById(UUID.fromString(ana)).orElseThrow().getAgency();
        String agent = member(AccountKind.SALES_AGENT, agency);
        String id = create(agent, lead("MILES", ana, ""), 201).path("referral").path("id").asText();
        assertThat(json(owner, get("/referrals/" + id), 200).path("history").get(0).path("author").asText()).contains("@test.local");
        assertThat(json(agent, get("/referrals/members"), 200).size()).isGreaterThanOrEqualTo(2);

        Agency other = agencies.save(Agency.builder().name("Indicações isolada").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.ACTIVE).build());
        CrmCustomer foreign = customers.save(CrmCustomer.builder().agency(other).name("Pessoa isolada").status(CustomerStatus.ACTIVE).notes("").build());
        PlatformAccount foreignUser = accounts.save(PlatformAccount.builder().agency(other).name("Outra equipe").email("indica-other-" + UUID.randomUUID() + "@test.local")
                .passwordHash(passwords.encode("x-password-123")).accountKind(AccountKind.AGENCY_OWNER).active(true).emailVerified(true).termsAccepted(true).build());
        create(owner, lead("TRAVEL", foreign.getId().toString(), ""), 404);
        create(owner, crm("TRAVEL", ana, foreign.getId().toString(), ""), 404);
        create(owner, lead("TRAVEL", ana, ",\"responsibleId\":\"" + foreignUser.getId() + "\""), 404);
        json(owner, body(put("/referrals/" + id + "/referred-customer/" + foreign.getId()), "{\"expectedVersion\":1}"), 404);
        assertThat(json(owner, get("/referrals/members"), 200).toString()).doesNotContain(foreignUser.getId().toString());

        UUID foreignReferral = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO referrals(id, agency_id, product, referrer_customer_id, referred_name, referred_email, referred_on, channel, created_by)
                VALUES (?, ?, 'TRAVEL', ?, 'Alheia', 'alheia@x.com', current_date, 'EVENT', 'teste')""", foreignReferral, other.getId(), foreign.getId());
        json(owner, get("/referrals/" + foreignReferral), 404);
        json(owner, body(post("/referrals/" + foreignReferral + "/status"), "{\"status\":\"CANCELLED\",\"reason\":\"x\",\"expectedVersion\":1}"), 404);
        json(owner, delete("/referrals/" + foreignReferral).param("expectedVersion", "1"), 404);
        assertThat(json(owner, get("/referrals"), 200).toString()).doesNotContain(foreignReferral.toString());
        assertThat(jdbc.queryForObject("SELECT status FROM referrals WHERE id = ?", String.class, foreignReferral)).isEqualTo("RECEIVED");

        JsonNode denied = json(member(AccountKind.PLATFORM_ADMIN, agency), get("/referrals"), 403);
        assertThat(denied.path("code").asText()).isEqualTo("REFERRAL_ACCESS_DENIED");
        mockMvc.perform(get("/referrals")).andExpect(IntegrationHttpSupport.unauthenticated());
    }
}
