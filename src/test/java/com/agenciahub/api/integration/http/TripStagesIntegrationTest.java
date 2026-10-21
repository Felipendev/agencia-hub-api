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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** F10 — etapas da viagem com banco embarcado e Flyway. */
class TripStagesIntegrationTest extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired AgencyRepository agencies;
    @Autowired CrmCustomerRepository customers;
    @Autowired PlatformAccountRepository accounts;
    @Autowired PasswordEncoder passwords;

    private static final String HOTEL = "{\"kind\":\"HOTEL\",\"title\":\"Hotel Central\",\"startsAt\":\"2099-01-10T14:00:00-03:00\","
            + "\"endsAt\":\"2099-01-12T12:00:00-03:00\",\"timeZone\":\"America/Fortaleza\",\"provider\":\"Rede X\",\"confirmationCode\":\"ABC123\",\"people\":2%s}";

    private MockHttpServletRequestBuilder as(String jwt, MockHttpServletRequestBuilder b) { return b.header("Authorization", "Bearer " + jwt); }

    private JsonNode json(String jwt, MockHttpServletRequestBuilder b, int expected) throws Exception {
        String body = mockMvc.perform(as(jwt, b)).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return body.isEmpty() ? null : http.parse(body);
    }

    private static MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder b, String json) {
        return b.contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private String trip(String jwt, String status) throws Exception {
        String person = json(jwt, body(post("/customers"), "{\"name\":\"Etapas %s\",\"status\":\"ACTIVE\"}".formatted(UUID.randomUUID())), 201).path("id").asText();
        return json(jwt, body(post("/trips"), ("{\"customerId\":\"%s\",\"serviceType\":\"AEREO\",\"status\":\"%s\","
                + "\"travelStartDate\":\"2099-01-10\",\"travelEndDate\":\"2099-01-15\"}").formatted(person, status)), 201).path("trip").path("id").asText();
    }

    private UUID agencyOf(String tripId) { return jdbc.queryForObject("SELECT agency_id FROM trips WHERE id = ?::uuid", UUID.class, tripId); }

    private UUID attachment(UUID agency, String tripId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO agency_attachments(id, agency_id, trip_id, original_filename, content_type, byte_size, content, description) VALUES (?, ?, ?::uuid, 'voucher.pdf', 'application/pdf', 1, ?, 'Voucher')",
                id, agency, tripId, new byte[]{1});
        return id;
    }

    private long count(String table) { return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class); }

    private String member(AccountKind kind, Agency agency) throws Exception {
        String email = "etapas-" + UUID.randomUUID() + "@test.local";
        accounts.save(PlatformAccount.builder().agency(agency).name("Equipe etapas").email(email)
                .passwordHash(passwords.encode("etapas-test-password")).accountKind(kind)
                .active(true).emailVerified(true).termsAccepted(true).build());
        String login = mockMvc.perform(body(post("/auth/login"), "{\"email\":\"%s\",\"password\":\"etapas-test-password\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return http.parse(login).path("token").asText();
    }

    @Test
    void stageCrudKeepsIdAuditsRescheduleAndHasNoSideEffects() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String trip = trip(jwt, "UPCOMING");
        long segments = count("trip_segments"), attachments = count("agency_attachments"), entries = count("financial_entries"), sales = count("sales");
        String base = "/trips/" + trip + "/stages";
        UUID key = UUID.randomUUID();
        JsonNode created = json(jwt, body(post(base).header("Idempotency-Key", key), HOTEL.formatted("")), 201);
        JsonNode again = json(jwt, body(post(base).header("Idempotency-Key", key), HOTEL.formatted("")), 201);
        String id = created.path("stage").path("id").asText();
        assertThat(id).isEqualTo(key.toString()).isEqualTo(again.path("stage").path("id").asText());
        assertThat(created.path("stage").path("startsAt").asText()).isEqualTo("2099-01-10T17:00:00Z");
        assertThat(created.path("stage").path("status").asText()).isEqualTo("PLANNED");
        assertThat(created.path("stage").path("warnings").size()).isZero();
        json(jwt, body(post(base).header("Idempotency-Key", key), HOTEL.formatted("").replace("Hotel Central", "Outro")), 409);
        json(jwt, body(post(base).header("Idempotency-Key", UUID.randomUUID()), HOTEL.formatted("").replace("2099-01-12T12", "2099-01-09T12")), 400);
        json(jwt, body(post(base).header("Idempotency-Key", UUID.randomUUID()), HOTEL.formatted(",\"origin\":\"Aeroporto\"")), 400);

        // Reagendar mantém o ID, incrementa a versão, audita o horário anterior e avisa fora do período da viagem.
        String moved = HOTEL.formatted(",\"expectedVersion\":1").replace("2099-01-12T12", "2099-01-17T12");
        JsonNode rescheduled = json(jwt, body(patch(base + "/" + id), moved), 200);
        assertThat(rescheduled.path("stage").path("id").asText()).isEqualTo(id);
        assertThat(rescheduled.path("stage").path("version").asInt()).isEqualTo(2);
        assertThat(rescheduled.path("stage").path("warnings").get(0).asText()).isEqualTo("Termina depois do retorno registrado na viagem.");
        assertThat(rescheduled.path("history").get(0).path("action").asText()).isEqualTo("RESCHEDULED");
        assertThat(rescheduled.path("history").get(0).path("detail").path("previousEndsAt").asText()).isEqualTo("2099-01-12T15:00Z");
        json(jwt, body(patch(base + "/" + id), moved), 409);

        // Persistência após recarregar e ordem cronológica.
        String transfer = "{\"kind\":\"TRANSFER\",\"title\":\"Aeroporto → hotel\",\"startsAt\":\"2099-01-10T11:00:00-03:00\",\"origin\":\"Aeroporto\",\"destination\":\"Hotel Central\"}";
        String transferId = json(jwt, body(post(base).header("Idempotency-Key", UUID.randomUUID()), transfer), 201).path("stage").path("id").asText();
        JsonNode itinerary = json(jwt, get(base), 200);
        assertThat(itinerary.path("stages").get(0).path("id").asText()).isEqualTo(transferId);
        assertThat(itinerary.path("stages").get(1).path("endsAt").asText()).isEqualTo("2099-01-17T15:00:00Z");
        assertThat(itinerary.path("trip").path("travelEndDate").asText()).isEqualTo("2099-01-15");

        // Exclusão auditada; chave excluída não recria.
        json(jwt, delete(base + "/" + transferId).param("expectedVersion", "2"), 409);
        json(jwt, delete(base + "/" + transferId).param("expectedVersion", "1"), 204);
        json(jwt, get(base + "/" + transferId), 404);
        json(jwt, body(post(base).header("Idempotency-Key", transferId), transfer), 409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM trip_stage_events WHERE stage_id = ?::uuid AND action = 'DELETED'", Long.class, transferId)).isEqualTo(1);

        assertThat(count("trip_segments")).isEqualTo(segments);
        assertThat(count("agency_attachments")).isEqualTo(attachments);
        assertThat(count("financial_entries")).isEqualTo(entries);
        assertThat(count("sales")).isEqualTo(sales);
    }

    @Test
    void statusTransitionsAndCancelledTripRules() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String trip = trip(jwt, "UPCOMING");
        String base = "/trips/" + trip + "/stages";
        String id = json(jwt, body(post(base).header("Idempotency-Key", UUID.randomUUID()), HOTEL.formatted("")), 201).path("stage").path("id").asText();
        assertThat(json(jwt, body(post(base + "/" + id + "/status"), "{\"status\":\"CONFIRMED\",\"expectedVersion\":1}"), 200).path("stage").path("status").asText()).isEqualTo("CONFIRMED");
        json(jwt, body(post(base + "/" + id + "/status"), "{\"status\":\"CANCELLED\",\"expectedVersion\":2}"), 400);
        JsonNode cancelled = json(jwt, body(post(base + "/" + id + "/status"), "{\"status\":\"CANCELLED\",\"reason\":\"Hotel sem vaga\",\"expectedVersion\":2}"), 200);
        assertThat(cancelled.path("stage").path("statusReason").asText()).isEqualTo("Hotel sem vaga");
        json(jwt, body(patch(base + "/" + id), HOTEL.formatted(",\"expectedVersion\":3")), 409);
        json(jwt, body(post(base + "/" + id + "/status"), "{\"status\":\"CONFIRMED\",\"reason\":\"x\",\"expectedVersion\":3}"), 400);
        JsonNode reactivated = json(jwt, body(post(base + "/" + id + "/status"), "{\"status\":\"PLANNED\",\"reason\":\"Vaga liberada\",\"expectedVersion\":3}"), 200);
        assertThat(reactivated.path("history").toString()).contains("CREATED", "STATUS_CONFIRMED", "STATUS_CANCELLED", "STATUS_PLANNED").doesNotContain("password");

        String cancelledTrip = trip(jwt, "CANCELLED");
        json(jwt, body(post("/trips/" + cancelledTrip + "/stages").header("Idempotency-Key", UUID.randomUUID()), HOTEL.formatted("")), 409);
        json(jwt, get("/trips/" + cancelledTrip + "/stages"), 200);
    }

    @Test
    void linksOnlyReferenceTripAttachmentsAndAgencyLibrary() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String trip = trip(jwt, "UPCOMING"), otherTrip = trip(jwt, "UPCOMING");
        UUID agency = agencyOf(trip);
        String base = "/trips/" + trip + "/stages";
        String id = json(jwt, body(post(base).header("Idempotency-Key", UUID.randomUUID()), HOTEL.formatted("")), 201).path("stage").path("id").asText();
        UUID voucher = attachment(agency, trip), foreignTripFile = attachment(agency, otherTrip);
        UUID guide = UUID.randomUUID();
        jdbc.update("INSERT INTO agency_library(id, agency_id, title, category, kind, url) VALUES (?, ?, 'Guia de Lisboa', 'OUTROS', 'LINK', 'https://example.com')", guide, agency);
        long files = count("agency_attachments"), library = count("agency_library");

        json(jwt, put(base + "/" + id + "/attachments/" + voucher), 200);
        JsonNode linked = json(jwt, put(base + "/" + id + "/attachments/" + voucher), 200);
        json(jwt, put(base + "/" + id + "/attachments/" + foreignTripFile), 404);
        linked = json(jwt, put(base + "/" + id + "/library/" + guide), 200);
        assertThat(linked.path("stage").path("links").size()).isEqualTo(2);
        assertThat(linked.path("stage").path("links").toString()).contains("voucher.pdf", "Guia de Lisboa", "ATTACHMENT", "LIBRARY");
        assertThat(count("agency_attachments")).isEqualTo(files);
        assertThat(count("agency_library")).isEqualTo(library);

        JsonNode unlinked = json(jwt, delete(base + "/" + id + "/attachments/" + voucher), 200);
        assertThat(unlinked.path("stage").path("links").size()).isEqualTo(1);
        assertThat(count("agency_attachments")).isEqualTo(files);
        assertThat(unlinked.path("history").toString()).contains("ATTACHMENT_LINKED", "ATTACHMENT_UNLINKED", "LIBRARY_LINKED");

        // Excluir o material na Biblioteca remove o vínculo e preserva a auditoria.
        jdbc.update("DELETE FROM agency_library WHERE id = ?", guide);
        assertThat(json(jwt, get(base + "/" + id), 200).path("stage").path("links").size()).isZero();
        // A etapa não é acessível por outra viagem da mesma agência.
        json(jwt, get("/trips/" + otherTrip + "/stages/" + id), 404);
    }

    @Test
    void agencyScopeAndRolesProtectStages() throws Exception {
        String owner = http.loginOwner(mockMvc);
        String trip = trip(owner, "UPCOMING");
        Agency agency = agencies.findById(agencyOf(trip)).orElseThrow();
        String agent = member(AccountKind.SALES_AGENT, agency);
        String base = "/trips/" + trip + "/stages";
        String id = json(agent, body(post(base).header("Idempotency-Key", UUID.randomUUID()), HOTEL.formatted("")), 201).path("stage").path("id").asText();
        assertThat(json(owner, get(base + "/" + id), 200).path("history").get(0).path("author").asText()).contains("@test.local");

        Agency other = agencies.save(Agency.builder().name("Etapas isolada").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.ACTIVE).build());
        CrmCustomer foreign = customers.save(CrmCustomer.builder().agency(other).name("Pessoa isolada").status(CustomerStatus.ACTIVE).notes("").build());
        UUID foreignTrip = UUID.randomUUID(), foreignStage = UUID.randomUUID();
        jdbc.update("INSERT INTO trips(id, agency_id, customer_id, service_type, status) VALUES (?, ?, ?, 'AEREO', 'UPCOMING')", foreignTrip, other.getId(), foreign.getId());
        jdbc.update("INSERT INTO trip_stages(id, agency_id, trip_id, kind, title, starts_at, created_by) VALUES (?, ?, ?, 'HOTEL', 'Alheio', now(), 'teste')",
                foreignStage, other.getId(), foreignTrip);
        UUID foreignFile = attachment(other.getId(), foreignTrip.toString());
        String foreignBase = "/trips/" + foreignTrip + "/stages";
        json(owner, get(foreignBase), 404);
        json(owner, body(post(foreignBase).header("Idempotency-Key", UUID.randomUUID()), HOTEL.formatted("")), 404);
        json(owner, get(foreignBase + "/" + foreignStage), 404);
        json(owner, body(post(foreignBase + "/" + foreignStage + "/status"), "{\"status\":\"CONFIRMED\",\"expectedVersion\":1}"), 404);
        json(owner, delete(foreignBase + "/" + foreignStage).param("expectedVersion", "1"), 404);
        json(owner, get(base + "/" + foreignStage), 404);
        json(owner, put(base + "/" + id + "/attachments/" + foreignFile), 404);
        assertThat(jdbc.queryForObject("SELECT status FROM trip_stages WHERE id = ?", String.class, foreignStage)).isEqualTo("PLANNED");

        JsonNode denied = json(member(AccountKind.PLATFORM_ADMIN, agency), get(base), 403);
        assertThat(denied.path("code").asText()).isEqualTo("TRIP_STAGE_ACCESS_DENIED");
        mockMvc.perform(get(base)).andExpect(IntegrationHttpSupport.unauthenticated());
    }
}
