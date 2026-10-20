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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** F07 — agenda e atas com banco embarcado e Flyway. */
class AgendaIntegrationTest extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired AgencyRepository agencies;
    @Autowired CrmCustomerRepository customers;
    @Autowired PlatformAccountRepository accounts;
    @Autowired PasswordEncoder passwords;

    private static final String PAST = "2026-01-10T10:00:00-03:00";
    private static final String FUTURE = "2099-01-10T10:00:00-03:00";
    // Cada teste usa um mês próprio para que avisos de sobreposição não dependam da ordem de execução.
    private static final String FUTURE_STATUS = "2099-02-10T10:00:00-03:00";
    private static final String FUTURE_PRODUCTS = "2099-03-10T10:00:00-03:00";
    private static final String FUTURE_SCOPE = "2099-04-10T10:00:00-03:00";

    private MockHttpServletRequestBuilder as(String jwt, MockHttpServletRequestBuilder b) { return b.header("Authorization", "Bearer " + jwt); }

    private JsonNode json(String jwt, MockHttpServletRequestBuilder b, int expected) throws Exception {
        String body = mockMvc.perform(as(jwt, b)).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return body.isEmpty() ? null : http.parse(body);
    }

    private static MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder b, String json) {
        return b.contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private String customer(String jwt) throws Exception {
        return json(jwt, body(post("/customers"), "{\"name\":\"Agenda %s\",\"status\":\"ACTIVE\"}".formatted(UUID.randomUUID())), 201).path("id").asText();
    }

    private String ownerId(String jwt) throws Exception {
        return json(jwt, get("/appointments/members"), 200).get(0).path("id").asText();
    }

    private static String appointment(String product, String customerId, String responsibleId, String startsAt, String extra) {
        return ("{\"product\":\"%s\",\"customerId\":\"%s\",\"responsibleId\":\"%s\",\"title\":\"Revisão do plano\",\"purpose\":\"Apresentar opções\","
                + "\"startsAt\":\"%s\",\"durationMinutes\":60,\"timeZone\":\"America/Fortaleza\",\"format\":\"VIDEO\",\"meetingUrl\":\"https://meet.example/x\"%s}")
                .formatted(product, customerId, responsibleId, startsAt, extra);
    }

    private JsonNode create(String jwt, String payload) throws Exception {
        return json(jwt, body(post("/appointments").header("Idempotency-Key", UUID.randomUUID()), payload), 201);
    }

    private long count(String table) { return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class); }

    private String member(AccountKind kind, Agency agency) throws Exception {
        String email = "agenda-" + UUID.randomUUID() + "@test.local";
        accounts.save(PlatformAccount.builder().agency(agency).name("Equipe agenda").email(email)
                .passwordHash(passwords.encode("agenda-test-password")).accountKind(kind)
                .active(true).emailVerified(true).termsAccepted(true).build());
        String login = mockMvc.perform(body(post("/auth/login"), "{\"email\":\"%s\",\"password\":\"agenda-test-password\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return http.parse(login).path("token").asText();
    }

    @Test
    void appointmentCrudIsIdempotentKeepsIdOnRescheduleAndWarnsAboutOverlaps() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        long trips = count("trips"), sales = count("sales"), quotations = count("quotations");
        String person = customer(jwt), owner = ownerId(jwt);
        UUID key = UUID.randomUUID();
        String payload = appointment("TRAVEL", person, owner, FUTURE, "");
        JsonNode created = json(jwt, body(post("/appointments").header("Idempotency-Key", key), payload), 201);
        JsonNode again = json(jwt, body(post("/appointments").header("Idempotency-Key", key), payload), 201);
        String id = created.path("appointment").path("id").asText();
        assertThat(id).isEqualTo(key.toString()).isEqualTo(again.path("appointment").path("id").asText());
        assertThat(created.path("appointment").path("startsAt").asText()).isEqualTo("2099-01-10T13:00:00Z");
        assertThat(created.path("appointment").path("timeZone").asText()).isEqualTo("America/Fortaleza");
        json(jwt, body(post("/appointments").header("Idempotency-Key", key), payload.replace("Revisão do plano", "Outro")), 409);

        // Sobreposição do mesmo responsável gera aviso, sem bloquear.
        JsonNode overlapping = create(jwt, appointment("MILES", person, owner, "2099-01-10T10:30:00-03:00", ""));
        assertThat(overlapping.path("overlaps").size()).isEqualTo(1);
        assertThat(overlapping.path("overlaps").get(0).path("id").asText()).isEqualTo(id);

        // Reagendar mantém o ID, incrementa a versão e audita o horário anterior.
        String moved = appointment("TRAVEL", person, owner, "2099-01-11T15:00:00-03:00", ",\"expectedVersion\":1");
        JsonNode rescheduled = json(jwt, body(patch("/appointments/" + id), moved), 200);
        assertThat(rescheduled.path("appointment").path("id").asText()).isEqualTo(id);
        assertThat(rescheduled.path("appointment").path("version").asInt()).isEqualTo(2);
        assertThat(rescheduled.path("overlaps").size()).isZero();
        assertThat(rescheduled.path("history").get(0).path("action").asText()).isEqualTo("RESCHEDULED");
        assertThat(rescheduled.path("history").get(0).path("detail").path("previousStartsAt").asText()).isEqualTo("2099-01-10T13:00Z");
        json(jwt, body(patch("/appointments/" + id), moved), 409);
        json(jwt, body(patch("/appointments/" + id), moved.replace("\"TRAVEL\"", "\"MILES\"").replace("\"expectedVersion\":1", "\"expectedVersion\":2")), 400);
        json(jwt, body(patch("/appointments/" + id), moved.replace(person, customer(jwt)).replace("\"expectedVersion\":1", "\"expectedVersion\":2")), 400);

        // Persistência após recarregar; exclusão restrita e auditada; chave excluída não recria.
        assertThat(json(jwt, get("/appointments/" + id), 200).path("appointment").path("startsAt").asText()).isEqualTo("2099-01-11T18:00:00Z");
        json(jwt, delete("/appointments/" + id).param("expectedVersion", "2"), 204);
        json(jwt, get("/appointments/" + id), 404);
        json(jwt, body(post("/appointments").header("Idempotency-Key", key), payload), 409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM appointment_events WHERE appointment_id = ?::uuid AND action = 'DELETED'", Long.class, id)).isEqualTo(1);

        assertThat(count("trips")).isEqualTo(trips);
        assertThat(count("sales")).isEqualTo(sales);
        assertThat(count("quotations")).isEqualTo(quotations);
    }

    @Test
    void statusMinutesVersionsAndCancellationPreserveHistory() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String person = customer(jwt), owner = ownerId(jwt);
        String future = create(jwt, appointment("TRAVEL", person, owner, FUTURE_STATUS, "")).path("appointment").path("id").asText();
        json(jwt, body(post("/appointments/" + future + "/status"), "{\"status\":\"COMPLETED\",\"expectedVersion\":1}"), 400);

        String id = create(jwt, appointment("TRAVEL", person, owner, PAST, "")).path("appointment").path("id").asText();
        String minutes = "{\"data\":{\"participants\":\"Ana, Bruno\",\"summary\":\"Discutimos o roteiro\",\"decisions\":[\"Lisboa em dezembro\"],"
                + "\"nextSteps\":[{\"description\":\"Enviar cotação de hotel\",\"owner\":\"Bruno\",\"dueOn\":\"2026-01-15\"}]},\"expectedVersion\":%d}";
        JsonNode first = json(jwt, body(put("/appointments/" + id + "/minutes"), minutes.formatted(0)), 200);
        assertThat(first.path("minutes").path("version").asInt()).isEqualTo(1);
        assertThat(json(jwt, body(put("/appointments/" + id + "/minutes"), minutes.formatted(1)), 200).path("minutes").path("version").asInt()).isEqualTo(1);
        JsonNode second = json(jwt, body(put("/appointments/" + id + "/minutes"), minutes.replace("Lisboa em dezembro", "Porto em dezembro").formatted(1)), 200);
        assertThat(second.path("minutesVersions").size()).isEqualTo(2);
        json(jwt, body(put("/appointments/" + id + "/minutes"), minutes.formatted(1)), 409);
        assertThat(json(jwt, get("/appointments/" + id + "/minutes/versions/1"), 200).path("data").path("decisions").get(0).asText()).isEqualTo("Lisboa em dezembro");
        json(jwt, body(put("/appointments/" + id + "/minutes"), "{\"data\":{\"summary\":\" \"},\"expectedVersion\":2}"), 400);

        // Ata localizável pela busca.
        assertThat(json(jwt, get("/appointments").param("customerId", person).param("q", "porto"), 200).toString()).contains(id).doesNotContain(future);

        json(jwt, body(post("/appointments/" + id + "/status"), "{\"status\":\"NO_SHOW\",\"expectedVersion\":1}"), 400);
        JsonNode done = json(jwt, body(post("/appointments/" + id + "/status"), "{\"status\":\"COMPLETED\",\"expectedVersion\":1}"), 200);
        assertThat(done.path("appointment").path("status").asText()).isEqualTo("COMPLETED");
        json(jwt, body(patch("/appointments/" + id), appointment("TRAVEL", person, owner, PAST, ",\"expectedVersion\":2")), 409);
        json(jwt, delete("/appointments/" + id).param("expectedVersion", "2"), 409);
        json(jwt, body(post("/appointments/" + id + "/status"), "{\"status\":\"SCHEDULED\",\"expectedVersion\":2}"), 400);
        JsonNode reopened = json(jwt, body(post("/appointments/" + id + "/status"), "{\"status\":\"SCHEDULED\",\"reason\":\"Marcado por engano\",\"expectedVersion\":2}"), 200);
        assertThat(reopened.path("history").get(0).path("action").asText()).isEqualTo("REOPENED");

        json(jwt, body(post("/appointments/" + id + "/status"), "{\"status\":\"CANCELLED\",\"expectedVersion\":3}"), 400);
        JsonNode cancelled = json(jwt, body(post("/appointments/" + id + "/status"), "{\"status\":\"CANCELLED\",\"reason\":\"Cliente remarcou por telefone\",\"expectedVersion\":3}"), 200);
        assertThat(cancelled.path("appointment").path("statusReason").asText()).isEqualTo("Cliente remarcou por telefone");
        assertThat(cancelled.path("minutes").path("data").path("decisions").get(0).asText()).isEqualTo("Porto em dezembro");
        json(jwt, body(post("/appointments/" + id + "/status"), "{\"status\":\"SCHEDULED\",\"reason\":\"x\",\"expectedVersion\":4}"), 400);
        json(jwt, body(put("/appointments/" + id + "/minutes"), minutes.formatted(2)), 409);
        assertThat(cancelled.path("history").toString()).contains("CREATED", "MINUTES_CREATED", "MINUTES_UPDATED", "STATUS_COMPLETED", "REOPENED", "STATUS_CANCELLED")
                .doesNotContain("password");

        String ics = mockMvc.perform(as(jwt, get("/appointments/" + id + "/ics"))).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/calendar")))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(ics).contains("BEGIN:VEVENT", "DTSTART:20260110T130000Z", "STATUS:CANCELLED", "SUMMARY:Revisão do plano");
    }

    @Test
    void productsTripsAndAgencyDayFiltersAreConsistent() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String person = customer(jwt), owner = ownerId(jwt);
        String tripPayload = "{\"customerId\":\"%s\",\"serviceType\":\"AEREO\",\"status\":\"UPCOMING\"}";
        String trip = json(jwt, body(post("/trips"), tripPayload.formatted(person)), 201).path("trip").path("id").asText();
        String otherTrip = json(jwt, body(post("/trips"), tripPayload.formatted(customer(jwt))), 201).path("trip").path("id").asText();

        json(jwt, body(post("/appointments").header("Idempotency-Key", UUID.randomUUID()), appointment("MILES", person, owner, FUTURE_PRODUCTS, ",\"tripId\":\"" + trip + "\"")), 400);
        json(jwt, body(post("/appointments").header("Idempotency-Key", UUID.randomUUID()), appointment("TRAVEL", person, owner, FUTURE_PRODUCTS, ",\"tripId\":\"" + otherTrip + "\"")), 404);
        json(jwt, body(post("/appointments").header("Idempotency-Key", UUID.randomUUID()), appointment("TRAVEL", person, owner, FUTURE_PRODUCTS, "").replace("\"product\":\"TRAVEL\",", "")), 400);
        String travel = create(jwt, appointment("TRAVEL", person, owner, FUTURE_PRODUCTS, ",\"tripId\":\"" + trip + "\"")).path("appointment").path("id").asText();
        // 23:30 em Fortaleza = 02:30Z do dia seguinte: pertence ao dia 01/11 da agência.
        String miles = create(jwt, appointment("MILES", person, owner, "2099-11-01T23:30:00-03:00", "")).path("appointment").path("id").asText();

        assertThat(json(jwt, get("/appointments").param("customerId", person).param("product", "MILES"), 200).toString()).contains(miles).doesNotContain(travel);
        assertThat(json(jwt, get("/appointments").param("customerId", person).param("from", "2099-11-01").param("to", "2099-11-01"), 200).toString()).contains(miles);
        assertThat(json(jwt, get("/appointments").param("customerId", person).param("from", "2099-11-02"), 200).toString()).doesNotContain(miles);
        json(jwt, get("/appointments").param("from", "2099-11-02").param("to", "2099-11-01"), 400);

        // Cancelar o atendimento de Milhas não altera o de Viagens nem a pessoa.
        json(jwt, body(post("/appointments/" + miles + "/status"), "{\"status\":\"CANCELLED\",\"reason\":\"Encerrou acompanhamento\",\"expectedVersion\":1}"), 200);
        assertThat(json(jwt, get("/appointments/" + travel), 200).path("appointment").path("status").asText()).isEqualTo("SCHEDULED");
        json(jwt, get("/customers/" + person), 200);

        OffsetDateTime stored = jdbc.queryForObject("SELECT starts_at FROM appointments WHERE id = ?::uuid", OffsetDateTime.class, miles);
        assertThat(stored.withOffsetSameInstant(ZoneOffset.UTC)).hasToString("2099-11-02T02:30Z");
    }

    @Test
    void agencyScopeAndRolesProtectTheAgenda() throws Exception {
        String owner = http.loginOwner(mockMvc);
        String person = customer(owner), ownerId = ownerId(owner);
        Agency agency = customers.findById(UUID.fromString(person)).orElseThrow().getAgency();
        String agent = member(AccountKind.SALES_AGENT, agency);
        String id = create(agent, appointment("TRAVEL", person, ownerId, FUTURE_SCOPE, "")).path("appointment").path("id").asText();
        assertThat(json(owner, get("/appointments/" + id), 200).path("history").get(0).path("author").asText()).contains("@test.local");
        assertThat(json(agent, get("/appointments/members"), 200).toString()).contains(ownerId);

        Agency other = agencies.save(Agency.builder().name("Agenda isolada").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.ACTIVE).build());
        CrmCustomer foreign = customers.save(CrmCustomer.builder().agency(other).name("Pessoa isolada").status(CustomerStatus.ACTIVE).notes("").build());
        PlatformAccount foreignUser = accounts.save(PlatformAccount.builder().agency(other).name("Outra equipe").email("agenda-other-" + UUID.randomUUID() + "@test.local")
                .passwordHash(passwords.encode("x-password-123")).accountKind(AccountKind.AGENCY_OWNER).active(true).emailVerified(true).termsAccepted(true).build());
        json(owner, body(post("/appointments").header("Idempotency-Key", UUID.randomUUID()), appointment("TRAVEL", foreign.getId().toString(), ownerId, FUTURE_SCOPE, "")), 404);
        json(owner, body(post("/appointments").header("Idempotency-Key", UUID.randomUUID()), appointment("TRAVEL", person, foreignUser.getId().toString(), FUTURE_SCOPE, "")), 404);
        assertThat(json(owner, get("/appointments/members"), 200).toString()).doesNotContain(foreignUser.getId().toString());

        UUID foreignAppointment = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO appointments(id, agency_id, customer_id, product, title, starts_at, duration_minutes, format, responsible_id, created_by)
                VALUES (?, ?, ?, 'TRAVEL', 'Alheio', now(), 30, 'PHONE', ?, 'teste')""", foreignAppointment, other.getId(), foreign.getId(), foreignUser.getId());
        json(owner, get("/appointments/" + foreignAppointment), 404);
        json(owner, body(post("/appointments/" + foreignAppointment + "/status"), "{\"status\":\"CANCELLED\",\"reason\":\"x\",\"expectedVersion\":1}"), 404);
        json(owner, body(put("/appointments/" + foreignAppointment + "/minutes"), "{\"data\":{\"summary\":\"x\"},\"expectedVersion\":0}"), 404);
        json(owner, delete("/appointments/" + foreignAppointment).param("expectedVersion", "1"), 404);
        assertThat(json(owner, get("/appointments"), 200).toString()).doesNotContain(foreignAppointment.toString());
        assertThat(jdbc.queryForObject("SELECT status FROM appointments WHERE id = ?", String.class, foreignAppointment)).isEqualTo("SCHEDULED");

        JsonNode denied = json(member(AccountKind.PLATFORM_ADMIN, agency), get("/appointments"), 403);
        assertThat(denied.path("code").asText()).isEqualTo("AGENDA_ACCESS_DENIED");
        mockMvc.perform(get("/appointments")).andExpect(IntegrationHttpSupport.unauthenticated());
    }
}
