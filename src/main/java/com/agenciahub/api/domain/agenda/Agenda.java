package com.agenciahub.api.domain.agenda;

import java.net.URI;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Agenda e atas de atendimento (F07). Regras puras: validação, transições de estado, texto de busca e ICS.
 * Nenhuma agenda externa é sincronizada; o ICS é um arquivo para importação manual.
 */
public final class Agenda {

    private Agenda() {}

    public static final ZoneId AGENCY_ZONE = ZoneId.of("America/Fortaleza");
    public static final int MIN_DURATION = 5;
    public static final int MAX_DURATION = 720;
    private static final int MAX_ITEMS = 50;

    /** Produto do atendimento: escolhido por quem cadastra, nunca deduzido. */
    public enum Product { TRAVEL, MILES }

    public enum Format { IN_PERSON, VIDEO, PHONE, OTHER }

    public enum Status { SCHEDULED, COMPLETED, NO_SHOW, CANCELLED }

    public record AppointmentInput(UUID customerId, Product product, UUID tripId, String title, String purpose,
                                   OffsetDateTime startsAt, Integer durationMinutes, String timeZone, Format format,
                                   String location, String meetingUrl, UUID responsibleId, Integer expectedVersion) {}

    public record StatusInput(Status status, String reason, Integer expectedVersion) {}

    public record NextStep(String description, String owner, LocalDate dueOn) {}

    public record Minutes(String participants, String summary, List<String> decisions, List<NextStep> nextSteps) {
        public List<String> decisions() { return decisions == null ? List.of() : decisions; }

        public List<NextStep> nextSteps() { return nextSteps == null ? List.of() : nextSteps; }
    }

    public record MinutesInput(Minutes data, Integer expectedVersion) {}

    // ------------------------------------------------------------------ validação

    /** Valida e normaliza: textos em branco viram {@code null}, início em UTC e fuso padrão da agência. */
    public static AppointmentInput normalize(AppointmentInput in) {
        if (in == null) invalid("Informe os dados do compromisso.");
        if (in.customerId() == null) invalid("Escolha a pessoa do atendimento.");
        if (in.product() == null) invalid("Informe se o atendimento é de Viagens ou de Milhas.");
        if (in.product() == Product.MILES && in.tripId() != null) invalid("Viagem só pode ser vinculada a atendimento de Viagens.");
        String title = required(in.title(), 180, "O título");
        String purpose = optional(in.purpose(), 2000, "O objetivo");
        if (in.startsAt() == null) invalid("Informe a data e o horário de início.");
        if (in.durationMinutes() == null || in.durationMinutes() < MIN_DURATION || in.durationMinutes() > MAX_DURATION) {
            invalid("A duração deve ficar entre 5 minutos e 12 horas.");
        }
        String zone = in.timeZone() == null || in.timeZone().isBlank() ? AGENCY_ZONE.getId() : in.timeZone().strip();
        try {
            ZoneId.of(zone);
        } catch (DateTimeException e) {
            invalid("Fuso horário inválido.");
        }
        if (zone.length() > 64) invalid("Fuso horário inválido.");
        if (in.format() == null) invalid("Informe o formato do atendimento.");
        String location = optional(in.location(), 300, "O local");
        String url = optional(in.meetingUrl(), 2048, "O link");
        if (url != null) requireHttpUrl(url);
        if (in.responsibleId() == null) invalid("Escolha o responsável pelo atendimento.");
        return new AppointmentInput(in.customerId(), in.product(), in.tripId(), title, purpose,
                in.startsAt().withOffsetSameInstant(ZoneOffset.UTC), in.durationMinutes(), zone, in.format(),
                location, url, in.responsibleId(), in.expectedVersion());
    }

    public static Minutes normalize(Minutes m) {
        if (m == null) invalid("Informe o conteúdo da ata.");
        String participants = optional(m.participants(), 1000, "Os participantes");
        String summary = optional(m.summary(), 20000, "O resumo");
        if (m.decisions().size() > MAX_ITEMS || m.nextSteps().size() > MAX_ITEMS) invalid("Use no máximo 50 decisões e 50 próximos passos.");
        List<String> decisions = m.decisions().stream().map(d -> optional(d, 1000, "Cada decisão")).filter(Objects::nonNull).toList();
        List<NextStep> steps = m.nextSteps().stream().map(step -> {
            if (step == null) return null;
            String description = optional(step.description(), 1000, "Cada próximo passo");
            if (description == null) return null;
            return new NextStep(description, optional(step.owner(), 200, "O responsável do próximo passo"), step.dueOn());
        }).filter(Objects::nonNull).toList();
        if (summary == null && decisions.isEmpty() && steps.isEmpty()) invalid("Registre ao menos o resumo, uma decisão ou um próximo passo.");
        return new Minutes(participants, summary, decisions, steps);
    }

    // ------------------------------------------------------------------ estados

    /** Transições permitidas; reabrir exige motivo. Cancelado é terminal. */
    public static void requireTransition(Status from, Status to, String reason) {
        if (to == null) invalid("Informe o novo estado.");
        if (from == to) return;
        boolean allowed = switch (from) {
            case SCHEDULED -> Set.of(Status.COMPLETED, Status.NO_SHOW, Status.CANCELLED).contains(to);
            case COMPLETED, NO_SHOW -> to == Status.SCHEDULED;
            case CANCELLED -> false;
        };
        if (!allowed) {
            invalid(from == Status.CANCELLED
                    ? "Compromisso cancelado não muda de estado. Crie um novo compromisso."
                    : "Mudança de estado não permitida.");
        }
        boolean needsReason = to == Status.CANCELLED || from != Status.SCHEDULED;
        if (needsReason && (reason == null || reason.isBlank())) {
            invalid(to == Status.CANCELLED ? "Informe o motivo do cancelamento." : "Informe o motivo para reabrir o compromisso.");
        }
        if (reason != null && reason.length() > 1000) invalid("O motivo pode ter até 1.000 caracteres.");
    }

    public static boolean minutesAllowed(Status status) {
        return status == Status.SCHEDULED || status == Status.COMPLETED;
    }

    public static boolean overlaps(Instant startA, int minutesA, Instant startB, int minutesB) {
        Instant endA = startA.plusSeconds(minutesA * 60L);
        Instant endB = startB.plusSeconds(minutesB * 60L);
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    // ------------------------------------------------------------------ busca e ICS

    /** Texto normalizado para busca simples (minúsculas), sem dados além da própria ata. */
    public static String searchText(Minutes m) {
        return Stream.concat(Stream.of(m.participants(), m.summary()),
                        Stream.concat(m.decisions().stream(), m.nextSteps().stream().flatMap(s -> Stream.of(s.description(), s.owner()))))
                .filter(Objects::nonNull).collect(Collectors.joining("\n")).toLowerCase(Locale.ROOT);
    }

    private static final DateTimeFormatter ICS_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    public record IcsEvent(UUID id, String title, String description, String location, String url, Instant start,
                           int durationMinutes, boolean cancelled, Instant stamp) {}

    public static String ics(IcsEvent e) {
        StringBuilder out = new StringBuilder();
        line(out, "BEGIN:VCALENDAR");
        line(out, "VERSION:2.0");
        line(out, "PRODID:-//AgenciaHub//Agenda//PT-BR");
        line(out, "METHOD:PUBLISH");
        line(out, "BEGIN:VEVENT");
        line(out, "UID:" + e.id() + "@agenciahub");
        line(out, "DTSTAMP:" + ICS_TIME.format(e.stamp()));
        line(out, "DTSTART:" + ICS_TIME.format(e.start()));
        line(out, "DTEND:" + ICS_TIME.format(e.start().plusSeconds(e.durationMinutes() * 60L)));
        line(out, "SUMMARY:" + escape(e.title()));
        if (e.description() != null) line(out, "DESCRIPTION:" + escape(e.description()));
        if (e.location() != null) line(out, "LOCATION:" + escape(e.location()));
        if (e.url() != null) line(out, "URL:" + e.url());
        line(out, "STATUS:" + (e.cancelled() ? "CANCELLED" : "CONFIRMED"));
        line(out, "END:VEVENT");
        line(out, "END:VCALENDAR");
        return out.toString();
    }

    static String escape(String text) {
        return text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
                .replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");
    }

    /** Linhas de até 75 octetos (RFC 5545), continuação com espaço, sem quebrar caracteres UTF-8. */
    private static void line(StringBuilder out, String content) {
        int octets = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < content.length(); ) {
            int cp = content.codePointAt(i);
            int size = new String(Character.toChars(cp)).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            if (octets + size > 75) {
                out.append(current).append("\r\n ");
                current.setLength(0);
                octets = 1;
            }
            current.appendCodePoint(cp);
            octets += size;
            i += Character.charCount(cp);
        }
        out.append(current).append("\r\n");
    }

    // ------------------------------------------------------------------ apoio

    private static void requireHttpUrl(String raw) {
        try {
            URI uri = URI.create(raw);
            String scheme = Objects.toString(uri.getScheme(), "").toLowerCase(Locale.ROOT);
            if (!Set.of("http", "https").contains(scheme) || uri.getHost() == null || uri.getUserInfo() != null) throw new IllegalArgumentException();
        } catch (RuntimeException e) {
            invalid("Informe o link da reunião começando com https:// ou http://.");
        }
    }

    private static String required(String value, int max, String name) {
        String text = optional(value, max, name);
        if (text == null) invalid(name + " é obrigatório.");
        return text;
    }

    private static String optional(String value, int max, String name) {
        if (value == null || value.isBlank()) return null;
        String text = value.strip();
        if (text.length() > max) invalid(name + " pode ter até " + max + " caracteres.");
        return text;
    }

    public static void invalid(String message) { throw new IllegalArgumentException(message); }
}
