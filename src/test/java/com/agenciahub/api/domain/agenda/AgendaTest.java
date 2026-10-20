package com.agenciahub.api.domain.agenda;

import com.agenciahub.api.domain.agenda.Agenda.*;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgendaTest {

    private static final UUID PERSON = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final OffsetDateTime START = OffsetDateTime.parse("2026-10-21T14:00:00-03:00");

    private static AppointmentInput input(Product product, UUID trip, String title, Integer duration, String zone, String url) {
        return new AppointmentInput(PERSON, product, trip, title, " ", START, duration, zone, Format.VIDEO, " ", url, USER, 0);
    }

    @Test
    void normalizesTextsStoresStartInUtcAndDefaultsToAgencyZone() {
        AppointmentInput n = Agenda.normalize(input(Product.TRAVEL, null, "  Revisão  ", 60, null, "https://meet.example/x"));
        assertThat(n.title()).isEqualTo("Revisão");
        assertThat(n.purpose()).isNull();
        assertThat(n.location()).isNull();
        assertThat(n.timeZone()).isEqualTo("America/Fortaleza");
        assertThat(n.startsAt()).hasToString("2026-10-21T17:00Z");
    }

    @Test
    void rejectsMissingProductMilesTripInvalidDurationZoneUrlAndTitle() {
        assertThatThrownBy(() -> Agenda.normalize(input(null, null, "t", 60, null, null))).hasMessageContaining("Viagens ou de Milhas");
        assertThatThrownBy(() -> Agenda.normalize(input(Product.MILES, UUID.randomUUID(), "t", 60, null, null))).hasMessageContaining("Viagem só");
        assertThatThrownBy(() -> Agenda.normalize(input(Product.TRAVEL, null, "t", 4, null, null))).hasMessageContaining("duração");
        assertThatThrownBy(() -> Agenda.normalize(input(Product.TRAVEL, null, "t", 721, null, null))).hasMessageContaining("duração");
        assertThatThrownBy(() -> Agenda.normalize(input(Product.TRAVEL, null, "t", 60, "Marte/Base", null))).hasMessageContaining("Fuso");
        assertThatThrownBy(() -> Agenda.normalize(input(Product.TRAVEL, null, "t", 60, null, "javascript:alert(1)"))).hasMessageContaining("https://");
        assertThatThrownBy(() -> Agenda.normalize(input(Product.TRAVEL, null, " ", 60, null, null))).hasMessageContaining("título");
    }

    @Test
    void statusTransitionsRequireReasonsAndCancelledIsTerminal() {
        Agenda.requireTransition(Status.SCHEDULED, Status.COMPLETED, null);
        Agenda.requireTransition(Status.SCHEDULED, Status.NO_SHOW, null);
        Agenda.requireTransition(Status.COMPLETED, Status.SCHEDULED, "Registrado por engano");
        assertThatThrownBy(() -> Agenda.requireTransition(Status.SCHEDULED, Status.CANCELLED, " ")).hasMessageContaining("motivo do cancelamento");
        assertThatThrownBy(() -> Agenda.requireTransition(Status.COMPLETED, Status.SCHEDULED, null)).hasMessageContaining("reabrir");
        assertThatThrownBy(() -> Agenda.requireTransition(Status.CANCELLED, Status.SCHEDULED, "x")).hasMessageContaining("cancelado");
        assertThatThrownBy(() -> Agenda.requireTransition(Status.NO_SHOW, Status.COMPLETED, "x")).hasMessageContaining("não permitida");
        assertThat(Agenda.minutesAllowed(Status.COMPLETED)).isTrue();
        assertThat(Agenda.minutesAllowed(Status.NO_SHOW)).isFalse();
    }

    @Test
    void minutesDropBlankItemsAndRequireContent() {
        Minutes m = Agenda.normalize(new Minutes(" Ana ", "Resumo", Arrays.asList("Decisão A", " "),
                Arrays.asList(new NextStep("Enviar cotação", "Bruno", LocalDate.of(2026, 10, 25)), new NextStep(" ", null, null))));
        assertThat(m.participants()).isEqualTo("Ana");
        assertThat(m.decisions()).containsExactly("Decisão A");
        assertThat(m.nextSteps()).hasSize(1);
        assertThat(Agenda.searchText(m)).contains("enviar cotação", "bruno", "decisão a");
        assertThatThrownBy(() -> Agenda.normalize(new Minutes(null, " ", List.of(), List.of()))).hasMessageContaining("ao menos");
    }

    @Test
    void overlapUsesHalfOpenIntervals() {
        Instant start = Instant.parse("2026-10-21T17:00:00Z");
        assertThat(Agenda.overlaps(start, 60, start.plusSeconds(1800), 60)).isTrue();
        assertThat(Agenda.overlaps(start, 60, start.plusSeconds(3600), 30)).isFalse();
    }

    @Test
    void icsEscapesTextUsesUtcAndFoldsLinesAt75Octets() {
        Instant start = Instant.parse("2026-10-21T17:00:00Z");
        String ics = Agenda.ics(new IcsEvent(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Revisão, milhas; ação",
                "Linha 1\nLinha 2 " + "é".repeat(60), null, "https://meet.example/x", start, 60, true, start));
        assertThat(ics).contains("DTSTART:20261021T170000Z", "DTEND:20261021T180000Z", "SUMMARY:Revisão\\, milhas\; ação",
                "Linha 1\\nLinha 2", "STATUS:CANCELLED", "UID:00000000-0000-0000-0000-000000000001@agenciahub");
        for (String line : ics.split("\r\n")) assertThat(line.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(75);
    }
}
