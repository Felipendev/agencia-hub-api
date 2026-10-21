package com.agenciahub.api.domain.stages;

import com.agenciahub.api.domain.stages.TripStages.Kind;
import com.agenciahub.api.domain.stages.TripStages.StageInput;
import com.agenciahub.api.domain.stages.TripStages.Status;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TripStagesTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2099-01-10T14:00:00-03:00");

    private static StageInput input(Kind kind, OffsetDateTime start, OffsetDateTime end, String zone, String origin, Integer people) {
        return new StageInput(kind, "  Hotel Central  ", start, end, zone, " ", null, null, origin, null, people, null, null);
    }

    @Test
    void normalizesTextZoneAndInstants() {
        StageInput out = TripStages.normalize(input(Kind.HOTEL, START, START.plusDays(2), null, null, 2));
        assertThat(out.title()).isEqualTo("Hotel Central");
        assertThat(out.provider()).isNull();
        assertThat(out.timeZone()).isEqualTo("America/Fortaleza");
        assertThat(out.startsAt()).hasToString("2099-01-10T17:00Z");
    }

    @Test
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> TripStages.normalize(input(Kind.HOTEL, START, START.minusHours(1), null, null, null)))
                .hasMessage("O check-out não pode ser anterior ao check-in.");
        assertThatThrownBy(() -> TripStages.normalize(input(Kind.ACTIVITY, null, null, null, null, null)))
                .hasMessage("Informe a data e o horário de início.");
        assertThatThrownBy(() -> TripStages.normalize(input(Kind.HOTEL, START, null, "Lua/Base", null, null))).hasMessage("Fuso horário inválido.");
        assertThatThrownBy(() -> TripStages.normalize(input(Kind.HOTEL, START, null, null, "Aeroporto", null)))
                .hasMessage("Origem e destino são usados apenas em transfer.");
        assertThatThrownBy(() -> TripStages.normalize(input(Kind.TRANSFER, START, null, null, "Aeroporto", 0))).hasMessage("Informe de 1 a 99 pessoas.");
        assertThatThrownBy(() -> TripStages.normalize(null)).hasMessage("Informe os dados da etapa.");
        assertThat(TripStages.normalize(input(Kind.TRANSFER, START, null, "Europe/Lisbon", "Aeroporto", 99)).origin()).isEqualTo("Aeroporto");
    }

    @Test
    void transitionsRequireReasonForCancelAndReactivation() {
        TripStages.requireTransition(Status.PLANNED, Status.CONFIRMED, null);
        TripStages.requireTransition(Status.CONFIRMED, Status.PLANNED, null);
        assertThatThrownBy(() -> TripStages.requireTransition(Status.CONFIRMED, Status.CANCELLED, " ")).hasMessage("Informe o motivo do cancelamento.");
        assertThatThrownBy(() -> TripStages.requireTransition(Status.CANCELLED, Status.PLANNED, null)).hasMessage("Informe o motivo para reativar a etapa.");
        assertThatThrownBy(() -> TripStages.requireTransition(Status.CANCELLED, Status.CONFIRMED, "ok"))
                .hasMessage("Reative a etapa como prevista antes de confirmá-la.");
        TripStages.requireTransition(Status.CANCELLED, Status.PLANNED, "Cliente manteve a reserva");
    }

    @Test
    void warnsOutsideTripPeriodUsingStageZone() {
        LocalDate from = LocalDate.parse("2099-01-10"), to = LocalDate.parse("2099-01-15");
        // 02:30Z do dia 10 ainda é dia 09 em Fortaleza.
        assertThat(TripStages.warnings(Instant.parse("2099-01-10T02:30:00Z"), null, "America/Fortaleza", from, to))
                .containsExactly("Começa antes da partida registrada na viagem.");
        assertThat(TripStages.warnings(Instant.parse("2099-01-10T02:30:00Z"), null, "Europe/Lisbon", from, to)).isEmpty();
        assertThat(TripStages.warnings(Instant.parse("2099-01-14T12:00:00Z"), Instant.parse("2099-01-16T12:00:00Z"), "America/Fortaleza", from, to))
                .containsExactly("Termina depois do retorno registrado na viagem.");
        assertThat(TripStages.warnings(Instant.parse("2099-01-01T12:00:00Z"), null, "America/Fortaleza", null, null)).isEmpty();
    }
}
