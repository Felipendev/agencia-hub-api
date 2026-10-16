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
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** F03 — economia realizada por viagem, com banco embarcado e Flyway. */
class TripSavingsIntegrationTest extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired AgencyRepository agencies;
    @Autowired CrmCustomerRepository customers;
    @Autowired PlatformAccountRepository accounts;
    @Autowired PasswordEncoder passwords;

    private static final String COMPLETE = """
            {"comparison":{"price":5000.00,"sourceType":"AIRLINE_SITE","sourceDescription":"Site da companhia","sourceUrl":"https://example.com/busca",
              "researchedOn":"2026-09-01","route":"FOR-LIS","outboundDate":"2026-12-01","returnDate":"2026-12-15","passengers":2,
              "fareConditions":"Econômica","baggage":"1 mala de 23 kg","sameRoute":true,"sameDates":true,"samePassengers":true,"equivalentConditions":true},
             "costs":[{"kind":"AGENCY_PAYMENT","amount":3000,"description":"Pago à agência"},
                      {"kind":"TAXES","amount":400,"description":"Taxas de embarque"},
                      {"kind":"SERVICE_FEE","amount":150.50,"description":"Honorários"}],
             "milesUsage":"NOT_USED","adjustments":[],"methodology":"Mesma rota, datas e bagagem pesquisadas no site da companhia","notes":"Cliente pediu janela"}""";

    private MockHttpServletRequestBuilder as(String jwt, MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + jwt);
    }

    private JsonNode json(String jwt, MockHttpServletRequestBuilder builder, int expected) throws Exception {
        String body = mockMvc.perform(as(jwt, builder)).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return body.isEmpty() ? null : http.parse(body);
    }

    private static MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder builder, String json) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private static String save(String data, int expectedVersion) {
        return "{\"data\":%s,\"expectedVersion\":%d}".formatted(data, expectedVersion);
    }

    private String customer(String jwt, String name) throws Exception {
        return json(jwt, body(post("/customers"), "{\"name\":\"%s %s\",\"status\":\"ACTIVE\"}".formatted(name, UUID.randomUUID())), 201).path("id").asText();
    }

    private String trip(String jwt, String customerId, String start, String status) throws Exception {
        String payload = "{\"customerId\":\"%s\",\"serviceType\":\"AEREO\",\"bookingLocator\":\"LOC%s\",\"status\":\"%s\",\"travelStartDate\":\"%s\",\"travelEndDate\":\"%s\"}"
                .formatted(customerId, UUID.randomUUID().toString().substring(0, 4).toUpperCase(), status, start, start);
        return json(jwt, body(post("/trips"), payload), 201).path("trip").path("id").asText();
    }

    private long count(String table) { return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class); }

    private String member(AccountKind kind, Agency agency) throws Exception {
        String email = "savings-" + UUID.randomUUID() + "@test.local";
        accounts.save(PlatformAccount.builder().agency(agency).name("Equipe economia").email(email)
                .passwordHash(passwords.encode("savings-test-password")).accountKind(kind)
                .active(true).emailVerified(true).termsAccepted(true).build());
        String login = mockMvc.perform(body(post("/auth/login"), "{\"email\":\"%s\",\"password\":\"savings-test-password\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return http.parse(login).path("token").asText();
    }

    @Test
    void analysisIsVersionedOnTheSameIdAndComputesOnlyDocumentedSavings() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        long sales = count("sales"), receivables = count("receivables"), entries = count("financial_entries"), quotations = count("quotations");
        String person = customer(jwt, "Economia");
        String tripId = trip(jwt, person, "2026-12-01", "UPCOMING");
        String base = "/trips/" + tripId + "/savings";

        JsonNode empty = json(jwt, get(base), 200);
        assertThat(empty.path("savingsId").isNull()).isTrue();
        assertThat(empty.path("result").path("available").asBoolean()).isFalse();
        assertThat(empty.path("result").path("reasons").toString()).contains("NO_ANALYSIS");

        // Comparação não equivalente: salva, mas sem número.
        String partial = COMPLETE.replace("\"sameDates\":true", "\"sameDates\":false");
        JsonNode created = json(jwt, body(put(base), save(partial, 0)), 200);
        String savingsId = created.path("savingsId").asText();
        assertThat(created.path("version").asInt()).isEqualTo(1);
        assertThat(created.path("result").path("available").asBoolean()).isFalse();
        assertThat(created.path("result").path("savings").isNull()).isTrue();
        assertThat(created.path("result").path("reasons").toString()).contains("NOT_EQUIVALENT");

        // Edição pelo mesmo ID cria a versão 2; repetir sem mudança não cria versão.
        JsonNode updated = json(jwt, body(put(base), save(COMPLETE, 1)), 200);
        assertThat(updated.path("savingsId").asText()).isEqualTo(savingsId);
        assertThat(updated.path("version").asInt()).isEqualTo(2);
        assertThat(updated.path("result").path("savings").decimalValue()).isEqualByComparingTo("1449.50");
        assertThat(updated.path("result").path("effectiveCost").decimalValue()).isEqualByComparingTo("3550.50");
        assertThat(json(jwt, body(put(base), save(COMPLETE, 2)), 200).path("version").asInt()).isEqualTo(2);
        json(jwt, body(put(base), save(COMPLETE.replace("5000.00", "5100.00"), 1)), 409);
        assertThat(json(jwt, get(base + "/versions/1"), 200).path("data").path("comparison").path("sameDates").asBoolean()).isFalse();

        // Reembolso e multa ajustam o resultado.
        String adjusted = COMPLETE.replace("\"adjustments\":[]",
                "\"adjustments\":[{\"kind\":\"REFUND\",\"amount\":200,\"date\":\"2026-10-01\",\"description\":\"Reembolso de taxa\"},"
                        + "{\"kind\":\"CANCELLATION_FEE\",\"amount\":50,\"date\":\"2026-10-02\",\"description\":\"Multa de trecho\"}]");
        JsonNode withAdjustments = json(jwt, body(put(base), save(adjusted, 2)), 200);
        assertThat(withAdjustments.path("result").path("effectiveCost").decimalValue()).isEqualByComparingTo("3400.50");
        assertThat(withAdjustments.path("result").path("savings").decimalValue()).isEqualByComparingTo("1599.50");

        // Milhas sem origem são recusadas; custo desconhecido torna indisponível.
        json(jwt, body(put(base), save(COMPLETE.replace("\"milesUsage\":\"NOT_USED\"", "\"milesUsage\":\"KNOWN\""), 3)), 400);
        JsonNode unknown = json(jwt, body(put(base), save(COMPLETE.replace("\"milesUsage\":\"NOT_USED\"", "\"milesUsage\":\"UNKNOWN\""), 3)), 200);
        assertThat(unknown.path("result").path("available").asBoolean()).isFalse();
        assertThat(unknown.path("result").path("reasons").toString()).contains("MILES_COST_UNKNOWN");

        // Persistência após recarregar e histórico.
        JsonNode reloaded = json(jwt, get(base), 200);
        assertThat(reloaded.path("versions").size()).isEqualTo(4);
        assertThat(reloaded.path("history").toString()).contains("SAVINGS_CREATED", "SAVINGS_UPDATED").doesNotContain("password");

        assertThat(count("sales")).isEqualTo(sales);
        assertThat(count("receivables")).isEqualTo(receivables);
        assertThat(count("financial_entries")).isEqualTo(entries);
        assertThat(count("quotations")).isEqualTo(quotations);
    }

    @Test
    void cancelledTripsLeaveTheRealizedTotalAndReportListsUnavailableReasons() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String person = customer(jwt, "Relatório");
        String done = trip(jwt, person, "2026-03-10", "COMPLETED");
        String cancelled = trip(jwt, person, "2026-04-10", "CANCELLED");
        String withoutAnalysis = trip(jwt, person, "2026-05-10", "COMPLETED");
        String outside = trip(jwt, person, "2025-01-10", "COMPLETED");
        for (String tripId : new String[]{done, cancelled, outside}) json(jwt, body(put("/trips/" + tripId + "/savings"), save(COMPLETE, 0)), 200);

        JsonNode report = json(jwt, get("/savings/report").param("customerId", person).param("from", "2026-01-01").param("to", "2026-12-31"), 200);
        assertThat(report.path("rows").size()).isEqualTo(3);
        assertThat(report.path("totals").path("available").asInt()).isEqualTo(1);
        assertThat(report.path("totals").path("cancelled").asInt()).isEqualTo(1);
        assertThat(report.path("totals").path("savings").decimalValue()).isEqualByComparingTo("1449.50");
        assertThat(report.toString()).contains("TRIP_CANCELLED", "NO_ANALYSIS").doesNotContain(outside);
        assertThat(json(jwt, get("/savings").param("customerId", person).param("available", "true"), 200).size()).isEqualTo(2);
        json(jwt, get("/savings/report").param("from", "2026-12-31").param("to", "2026-01-01"), 400);
        assertThat(withoutAnalysis).isNotBlank();
    }

    @Test
    void sharedReportIsAnImmutableRedactedSnapshotGeneratedByTheOwner() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String person = customer(jwt, "Compartilhado");
        String tripId = trip(jwt, person, "2026-06-10", "COMPLETED");
        json(jwt, body(put("/trips/" + tripId + "/savings"), save(COMPLETE, 0)), 200);
        String locator = json(jwt, get("/trips/" + tripId), 200).path("trip").path("bookingLocator").asText();
        String personName = json(jwt, get("/customers/" + person), 200).path("name").asText();

        String request = "{\"customerId\":\"%s\",\"from\":\"2026-01-01\",\"to\":\"2026-12-31\",\"options\":{\"hideCustomerName\":true,\"hideLocators\":true,\"hideSources\":true,\"hideNotes\":true,\"title\":\"Resultado 2026\"}}"
                .formatted(person);
        JsonNode preview = json(jwt, body(post("/savings/shared-reports/preview"), request), 200);
        assertThat(preview.toString()).doesNotContain(personName, locator, "example.com", "Cliente pediu janela").contains("Resultado 2026");
        assertThat(count("trip_savings_reports")).isZero();

        String reviewed = request.substring(0, request.length() - 1) + ",\"expectedPreviewHash\":\"" + preview.path("previewHash").asText() + "\"}";
        JsonNode shared = json(jwt, body(post("/savings/shared-reports"), reviewed), 201);
        String id = shared.path("id").asText();
        assertThat(shared.path("content").path("customerLabel").asText()).isEqualTo("Cliente");
        assertThat(shared.path("content").path("totals").path("savings").decimalValue()).isEqualByComparingTo("1449.50");

        // Editar a análise depois não altera o retrato gerado.
        json(jwt, body(put("/trips/" + tripId + "/savings"), save(COMPLETE.replace("5000.00", "9000.00"), 1)), 200);
        assertThat(json(jwt, get("/savings/shared-reports/" + id), 200).path("content").path("totals").path("savings").decimalValue())
                .isEqualByComparingTo("1449.50");

        byte[] pdf = mockMvc.perform(as(jwt, get("/savings/shared-reports/" + id + "/pdf")))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Resultado 2026", "1.449,50").doesNotContain(personName, locator);
        }
        assertThat(json(jwt, get("/savings/shared-reports").param("customerId", person), 200).size()).isEqualTo(1);
        json(jwt, delete("/savings/shared-reports/" + id), 204);
        json(jwt, get("/savings/shared-reports/" + id), 404);
    }

    @Test
    void generationRequiresTheReviewedSnapshotAndRejectsChangedSources() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String person = customer(jwt, "Prévia consistente");
        String tripId = trip(jwt, person, "2026-06-10", "COMPLETED");
        json(jwt, body(put("/trips/" + tripId + "/savings"), save(COMPLETE, 0)), 200);
        String request = "{\"customerId\":\"%s\",\"options\":{\"hideCustomerName\":true,\"hideLocators\":true,\"hideSources\":true,\"hideNotes\":true}}".formatted(person);
        JsonNode preview = json(jwt, body(post("/savings/shared-reports/preview"), request), 200);
        json(jwt, body(post("/savings/shared-reports"), request), 409);
        String reviewed = request.substring(0, request.length() - 1) + ",\"expectedPreviewHash\":\"" + preview.path("previewHash").asText() + "\"}";
        json(jwt, body(put("/trips/" + tripId + "/savings"), save(COMPLETE.replace("3000", "3200"), 1)), 200);
        json(jwt, body(post("/savings/shared-reports"), reviewed), 409);
        JsonNode fresh = json(jwt, body(post("/savings/shared-reports/preview"), request), 200);
        reviewed = request.substring(0, request.length() - 1) + ",\"expectedPreviewHash\":\"" + fresh.path("previewHash").asText() + "\"}";
        JsonNode shared = json(jwt, body(post("/savings/shared-reports"), reviewed), 201);
        assertThat(shared.path("content")).isEqualTo(fresh);
        json(jwt, delete("/savings/shared-reports/" + shared.path("id").asText()), 204);
    }

    @Test
    void retainedReportsBlockCustomerDeletionAndPartialAnonymization() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String person = customer(jwt, "Relatório preservado");
        String email = "savings-" + UUID.randomUUID() + "@test.local";
        json(jwt, body(patch("/customers/" + person), "{\"email\":\"%s\"}".formatted(email)), 200);
        String tripId = trip(jwt, person, "2026-06-10", "COMPLETED");
        json(jwt, body(put("/trips/" + tripId + "/savings"), save(COMPLETE, 0)), 200);
        String request = "{\"customerId\":\"%s\",\"options\":{}}".formatted(person);
        JsonNode preview = json(jwt, body(post("/savings/shared-reports/preview"), request), 200);
        request = request.substring(0, request.length() - 1) + ",\"expectedPreviewHash\":\"" + preview.path("previewHash").asText() + "\"}";
        String reportId = json(jwt, body(post("/savings/shared-reports"), request), 201).path("id").asText();
        json(jwt, delete("/trips/" + tripId), 204);
        assertThat(json(jwt, get("/savings/shared-reports/" + reportId), 200).path("content").path("lines").size()).isEqualTo(1);
        assertThat(json(jwt, delete("/customers/" + person), 409).path("code").asText()).isEqualTo("SAVINGS_REPORT_HISTORY_EXISTS");
        String deletionId = json(jwt, body(post("/public/data-deletion-request"), "{\"email\":\"%s\"}".formatted(email)), 201).path("requestId").asText();
        JsonNode blocked = json(jwt, body(post("/agency/data-deletion-requests/" + deletionId + "/process"), "{\"acao\":\"EXCLUIR\"}"), 400);
        assertThat(blocked.path("message").asText()).contains("continua pendente");
        assertThat(jdbc.queryForObject("SELECT status FROM data_deletion_requests WHERE id=?::uuid", String.class, deletionId)).isEqualTo("PENDING");
        assertThat(json(jwt, get("/customers/" + person), 200).path("email").asText()).isEqualTo(email);
        json(jwt, delete("/savings/shared-reports/" + reportId), 204);
    }

    @Test
    void evidenceUploadsAreValidatedAndDeletingTheAnalysisIsAudited() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String tripId = trip(jwt, customer(jwt, "Evidência"), "2026-07-10", "UPCOMING");
        String base = "/trips/" + tripId + "/savings";
        mockMvc.perform(as(jwt, multipart(base + "/evidence").file(new MockMultipartFile("file", "print.png", "image/png", new byte[]{1}))))
                .andExpect(status().isBadRequest());
        String savingsId = json(jwt, body(put(base), save(COMPLETE, 0)), 200).path("savingsId").asText();
        mockMvc.perform(as(jwt, multipart(base + "/evidence").file(new MockMultipartFile("file", "x.exe", "application/x-msdownload", new byte[]{1}))))
                .andExpect(status().isBadRequest());
        String evidence = mockMvc.perform(as(jwt, multipart(base + "/evidence").file(new MockMultipartFile("file", "print.png", "image/png", new byte[]{1, 2}))
                        .param("description", "Busca no site")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String evidenceId = http.parse(evidence).path("id").asText();
        mockMvc.perform(as(jwt, get(base + "/evidence/" + evidenceId))).andExpect(status().isOk()).andExpect(header().string("X-Content-Type-Options", "nosniff"));
        json(jwt, delete(base + "/evidence/" + evidenceId), 204);

        json(jwt, delete(base).param("expectedVersion", "9"), 409);
        json(jwt, delete(base).param("expectedVersion", "1"), 204);
        assertThat(json(jwt, get(base), 200).path("savingsId").isNull()).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM trip_savings_audit WHERE resource_id = ?::uuid AND action = 'SAVINGS_DELETED'", Long.class, savingsId)).isEqualTo(1);
        assertThat(count("trip_savings_versions WHERE savings_id = '" + savingsId + "'")).isZero();
    }

    @Test
    void agencyScopeAndRolesProtectAnalysesAndReports() throws Exception {
        String owner = http.loginOwner(mockMvc);
        String person = customer(owner, "Equipe");
        String tripId = trip(owner, person, "2026-08-10", "UPCOMING");
        Agency agency = customers.findById(UUID.fromString(person)).orElseThrow().getAgency();
        String agent = member(AccountKind.SALES_AGENT, agency);

        assertThat(json(agent, body(put("/trips/" + tripId + "/savings"), save(COMPLETE, 0)), 200).path("version").asInt()).isEqualTo(1);
        json(agent, get("/savings/report").param("customerId", person), 200);
        String request = "{\"customerId\":\"%s\",\"options\":{\"hideCustomerName\":false,\"hideLocators\":false,\"hideSources\":false,\"hideNotes\":false}}".formatted(person);
        JsonNode denied = json(agent, body(post("/savings/shared-reports"), request), 403);
        assertThat(denied.path("code").asText()).isEqualTo("SAVINGS_ACCESS_DENIED");
        json(member(AccountKind.PLATFORM_ADMIN, agency), get("/savings"), 403);
        mockMvc.perform(get("/savings")).andExpect(IntegrationHttpSupport.unauthenticated());

        Agency other = agencies.save(Agency.builder().name("Economia isolada").status(AgencyStatus.ACTIVE).subscriptionStatus(SubscriptionStatus.ACTIVE).build());
        CrmCustomer foreign = customers.save(CrmCustomer.builder().agency(other).name("Pessoa isolada").status(CustomerStatus.ACTIVE).notes("").build());
        UUID foreignTrip = UUID.randomUUID();
        jdbc.update("INSERT INTO trips(id, agency_id, customer_id, service_type, status) VALUES (?, ?, ?, 'AEREO', 'UPCOMING')", foreignTrip, other.getId(), foreign.getId());
        json(owner, get("/trips/" + foreignTrip + "/savings"), 404);
        json(owner, body(put("/trips/" + foreignTrip + "/savings"), save(COMPLETE, 0)), 404);
        json(owner, delete("/trips/" + foreignTrip + "/savings").param("expectedVersion", "1"), 404);
        json(owner, body(post("/savings/shared-reports/preview"), request.replace(person, foreign.getId().toString())), 404);
        assertThat(json(owner, get("/savings"), 200).toString()).doesNotContain(foreignTrip.toString());
        assertThat(count("trip_savings WHERE trip_id = '" + foreignTrip + "'")).isZero();
    }
}
