package com.agenciahub.api.domain.stages;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Etapas da viagem (hotel, transfer, atividade). Regras puras de validação, estado e avisos.
 * Não representa trechos aéreos nem gera efeito financeiro.
 */
public final class TripStages {

    private TripStages() {}

    public static final ZoneId AGENCY_ZONE = ZoneId.of("America/Fortaleza");

    public enum Kind { HOTEL, TRANSFER, ACTIVITY, OTHER }

    public enum Status { PLANNED, CONFIRMED, CANCELLED }

    public record StageInput(Kind kind, String title, OffsetDateTime startsAt, OffsetDateTime endsAt, String timeZone,
                             String provider, String confirmationCode, String location, String origin, String destination,
                             Integer people, String notes, Integer expectedVersion) {}

    public record StatusInput(Status status, String reason, Integer expectedVersion) {}

    /** Valida e normaliza: textos em branco viram {@code null}, instantes em UTC e fuso padrão da agência. */
    public static StageInput normalize(StageInput in) {
        if (in == null) invalid("Informe os dados da etapa.");
        if (in.kind() == null) invalid("Informe o tipo da etapa (hotel, transfer, atividade ou outro).");
        String title = text(in.title(), 180, "O título");
        if (title == null) invalid("O título é obrigatório.");
        if (in.startsAt() == null) invalid(in.kind() == Kind.HOTEL ? "Informe a data e o horário de check-in." : "Informe a data e o horário de início.");
        if (in.endsAt() != null && in.endsAt().isBefore(in.startsAt())) {
            invalid(in.kind() == Kind.HOTEL ? "O check-out não pode ser anterior ao check-in." : "O fim não pode ser anterior ao início.");
        }
        String zone = in.timeZone() == null || in.timeZone().isBlank() ? AGENCY_ZONE.getId() : in.timeZone().strip();
        try {
            if (zone.length() > 64) throw new DateTimeException("long");
            ZoneId.of(zone);
        } catch (DateTimeException e) {
            invalid("Fuso horário inválido.");
        }
        String origin = text(in.origin(), 300, "A origem");
        String destination = text(in.destination(), 300, "O destino");
        if (in.kind() != Kind.TRANSFER && (origin != null || destination != null)) invalid("Origem e destino são usados apenas em transfer.");
        if (in.people() != null && (in.people() < 1 || in.people() > 99)) invalid("Informe de 1 a 99 pessoas.");
        return new StageInput(in.kind(), title, in.startsAt().withOffsetSameInstant(ZoneOffset.UTC),
                in.endsAt() == null ? null : in.endsAt().withOffsetSameInstant(ZoneOffset.UTC), zone,
                text(in.provider(), 200, "O fornecedor"), text(in.confirmationCode(), 100, "O código de confirmação"),
                text(in.location(), 500, "O local"), origin, destination, in.people(), text(in.notes(), 4000, "As observações"),
                in.expectedVersion());
    }

    /** Prevista ⇄ confirmada livremente; cancelar exige motivo; cancelada só volta a prevista com motivo. */
    public static void requireTransition(Status from, Status to, String reason) {
        if (to == null) invalid("Informe a nova situação.");
        if (from == to) return;
        boolean needsReason = to == Status.CANCELLED || from == Status.CANCELLED;
        if (from == Status.CANCELLED && to == Status.CONFIRMED) invalid("Reative a etapa como prevista antes de confirmá-la.");
        if (needsReason && (reason == null || reason.isBlank())) {
            invalid(to == Status.CANCELLED ? "Informe o motivo do cancelamento." : "Informe o motivo para reativar a etapa.");
        }
        if (reason != null && reason.length() > 1000) invalid("O motivo pode ter até 1.000 caracteres.");
    }

    /** Avisos sem bloqueio: etapa fora do período registrado da viagem, comparando datas no fuso da etapa. */
    public static List<String> warnings(Instant start, Instant end, String zone, LocalDate tripStart, LocalDate tripEnd) {
        List<String> out = new ArrayList<>();
        ZoneId id = ZoneId.of(zone);
        LocalDate startDay = start.atZone(id).toLocalDate();
        LocalDate endDay = end == null ? startDay : end.atZone(id).toLocalDate();
        if (tripStart != null && startDay.isBefore(tripStart)) out.add("Começa antes da partida registrada na viagem.");
        if (tripEnd != null && endDay.isAfter(tripEnd)) out.add("Termina depois do retorno registrado na viagem.");
        return out;
    }

    private static String text(String value, int max, String name) {
        if (value == null || value.isBlank()) return null;
        String stripped = value.strip();
        if (stripped.length() > max) invalid(name + " pode ter até " + max + " caracteres.");
        return stripped;
    }

    public static void invalid(String message) { throw new IllegalArgumentException(message); }
}
