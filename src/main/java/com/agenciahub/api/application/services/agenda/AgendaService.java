package com.agenciahub.api.application.services.agenda;

import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.domain.agenda.Agenda;
import com.agenciahub.api.domain.agenda.Agenda.*;
import com.agenciahub.api.exception.AgendaConflictException;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.security.SecurityContextUsers;
import com.agenciahub.api.security.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static com.agenciahub.api.domain.agenda.Agenda.invalid;

/**
 * Compromissos e atas de atendimento por pessoa e produto. Não cria tarefas, vendas, viagens
 * nem eventos em agendas externas.
 */
@Service
@RequiredArgsConstructor
public class AgendaService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public record Member(UUID id, String name) {}

    public record AppointmentView(UUID id, Product product, UUID customerId, String customerName, UUID tripId, String title,
                                  String purpose, OffsetDateTime startsAt, OffsetDateTime endsAt, int durationMinutes,
                                  String timeZone, Format format, String location, String meetingUrl, UUID responsibleId,
                                  String responsibleName, Status status, String statusReason, OffsetDateTime statusChangedAt,
                                  int version, Integer minutesVersion, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}

    public record MinutesView(Minutes data, int version, OffsetDateTime updatedAt) {}

    public record VersionMeta(int version, String author, OffsetDateTime createdAt) {}

    public record MinutesVersion(int version, Minutes data, String author, OffsetDateTime createdAt) {}

    public record Overlap(UUID id, String title, OffsetDateTime startsAt, int durationMinutes) {}

    public record Event(UUID id, String action, Object detail, String author, OffsetDateTime createdAt) {}

    public record Detail(AppointmentView appointment, MinutesView minutes, List<VersionMeta> minutesVersions,
                         List<Overlap> overlaps, List<Event> history) {}

    public record Filters(UUID customerId, Product product, Status status, UUID responsibleId, LocalDate from, LocalDate to, String q) {}

    private static final String SELECT = """
            SELECT a.*, p.name AS customer_name, u.name AS responsible_name, m.version AS minutes_version
            FROM appointments a
            JOIN customers p ON p.id = a.customer_id AND p.agency_id = a.agency_id
            JOIN users u ON u.id = a.responsible_id
            LEFT JOIN appointment_minutes m ON m.appointment_id = a.id AND m.agency_id = a.agency_id
            WHERE a.agency_id = ?""";

    private static final RowMapper<AppointmentView> ROW = (rs, n) -> {
        OffsetDateTime start = rs.getObject("starts_at", OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC);
        int duration = rs.getInt("duration_minutes");
        return new AppointmentView(rs.getObject("id", UUID.class), Product.valueOf(rs.getString("product")),
                rs.getObject("customer_id", UUID.class), rs.getString("customer_name"), rs.getObject("trip_id", UUID.class),
                rs.getString("title"), rs.getString("purpose"), start, start.plusMinutes(duration), duration,
                rs.getString("time_zone"), Format.valueOf(rs.getString("format")), rs.getString("location"),
                rs.getString("meeting_url"), rs.getObject("responsible_id", UUID.class), rs.getString("responsible_name"),
                Status.valueOf(rs.getString("status")), rs.getString("status_reason"),
                rs.getObject("status_changed_at", OffsetDateTime.class), rs.getInt("version"),
                (Integer) rs.getObject("minutes_version"), rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class));
    };

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public List<Member> members() {
        return jdbc.query("""
                SELECT id, name FROM users WHERE agency_id = ? AND active = true AND account_kind IN ('AGENCY_OWNER','SALES_AGENT')
                ORDER BY lower(name), id""", (rs, n) -> new Member(rs.getObject(1, UUID.class), rs.getString(2)), agency());
    }

    @Transactional(readOnly = true)
    public List<AppointmentView> list(Filters f) {
        if (f.from() != null && f.to() != null && f.to().isBefore(f.from())) invalid("O fim do período não pode ser anterior ao início.");
        StringBuilder sql = new StringBuilder(SELECT);
        List<Object> args = new ArrayList<>(List.of(agency()));
        if (f.customerId() != null) { sql.append(" AND a.customer_id = ?"); args.add(f.customerId()); }
        if (f.product() != null) { sql.append(" AND a.product = ?"); args.add(f.product().name()); }
        if (f.status() != null) { sql.append(" AND a.status = ?"); args.add(f.status().name()); }
        if (f.responsibleId() != null) { sql.append(" AND a.responsible_id = ?"); args.add(f.responsibleId()); }
        // Datas do filtro são dias no fuso da agência; fim inclusivo.
        if (f.from() != null) { sql.append(" AND a.starts_at >= ?"); args.add(f.from().atStartOfDay(Agenda.AGENCY_ZONE).toOffsetDateTime()); }
        if (f.to() != null) { sql.append(" AND a.starts_at < ?"); args.add(f.to().plusDays(1).atStartOfDay(Agenda.AGENCY_ZONE).toOffsetDateTime()); }
        if (f.q() != null && !f.q().isBlank()) {
            if (f.q().length() > 200) invalid("A busca pode ter até 200 caracteres.");
            String like = "%" + f.q().strip().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            sql.append(" AND (lower(a.title) LIKE ? OR lower(coalesce(a.purpose,'')) LIKE ? OR coalesce((SELECT x.search_text FROM appointment_minutes x WHERE x.appointment_id = a.id),'') LIKE ?)");
            args.add(like); args.add(like); args.add(like);
        }
        sql.append(" ORDER BY a.starts_at, a.id LIMIT 500");
        return jdbc.query(sql.toString(), ROW, args.toArray());
    }

    @Transactional(readOnly = true)
    public Detail detail(UUID id) {
        return detail(find(id, false));
    }

    @Transactional(readOnly = true)
    public MinutesVersion minutesVersion(UUID id, int version) {
        find(id, false);
        return jdbc.query("SELECT version, data, author, created_at FROM appointment_minutes_versions WHERE appointment_id = ? AND agency_id = ? AND version = ?",
                        (rs, n) -> new MinutesVersion(rs.getInt(1), read(rs.getString(2), Minutes.class), rs.getString(3), rs.getObject(4, OffsetDateTime.class)),
                        id, agency(), version).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Versão da ata não encontrada."));
    }

    @Transactional(readOnly = true)
    public String ics(UUID id) {
        AppointmentView a = find(id, false);
        String description = a.purpose() == null ? null : a.purpose() + (a.meetingUrl() == null ? "" : "\n" + a.meetingUrl());
        return Agenda.ics(new IcsEvent(a.id(), a.title(), description, a.location(), a.meetingUrl(), a.startsAt().toInstant(),
                a.durationMinutes(), a.status() == Status.CANCELLED, Instant.now()));
    }

    // ------------------------------------------------------------------ escrita

    @Transactional
    public Detail create(UUID requestKey, AppointmentInput raw) {
        AppointmentInput in = Agenda.normalize(raw);
        lockCustomer(in.customerId());
        Optional<AppointmentView> existing = jdbc.query(SELECT + " AND a.id = ?", ROW, agency(), requestKey).stream().findFirst();
        if (existing.isPresent()) {
            AppointmentView a = existing.get();
            if (!a.customerId().equals(in.customerId()) || a.product() != in.product() || !a.title().equals(in.title())
                    || !a.startsAt().toInstant().equals(in.startsAt().toInstant())) {
                throw new AgendaConflictException("A confirmação já foi utilizada para outro compromisso.");
            }
            return detail(a);
        }
        Boolean deleted = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM appointment_events WHERE agency_id = ? AND appointment_id = ? AND action = 'DELETED')",
                Boolean.class, agency(), requestKey);
        if (Boolean.TRUE.equals(deleted)) throw new AgendaConflictException("Este compromisso já foi excluído. Recarregue a página antes de criar outro.");
        validateReferences(in);
        jdbc.update("""
                INSERT INTO appointments(id, agency_id, customer_id, product, trip_id, title, purpose, starts_at, duration_minutes,
                    time_zone, format, location, meeting_url, responsible_id, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                requestKey, agency(), in.customerId(), in.product().name(), in.tripId(), in.title(), in.purpose(), in.startsAt(),
                in.durationMinutes(), in.timeZone(), in.format().name(), in.location(), in.meetingUrl(), in.responsibleId(), author());
        AppointmentView created = find(requestKey, false);
        event(created, "CREATED", snapshot(created));
        return detail(created);
    }

    /** Edita pelo mesmo ID enquanto agendado; reagendamento registra o horário anterior. */
    @Transactional
    public Detail update(UUID id, AppointmentInput raw) {
        AppointmentView before = find(id, true);
        if (before.status() != Status.SCHEDULED) {
            throw new AgendaConflictException("Somente compromissos agendados podem ser editados. Reabra o compromisso para alterar.");
        }
        expectVersion(before.version(), raw == null ? null : raw.expectedVersion());
        AppointmentInput in = Agenda.normalize(raw);
        if (!before.customerId().equals(in.customerId())) invalid("A pessoa do compromisso não pode ser trocada. Crie outro compromisso.");
        if (before.product() != in.product()) invalid("O produto do compromisso não pode ser trocado. Crie outro compromisso.");
        validateReferences(in);
        boolean rescheduled = !before.startsAt().toInstant().equals(in.startsAt().toInstant()) || before.durationMinutes() != in.durationMinutes()
                || !before.timeZone().equals(in.timeZone());
        boolean unchanged = !rescheduled && Objects.equals(before.tripId(), in.tripId()) && before.title().equals(in.title())
                && Objects.equals(before.purpose(), in.purpose()) && before.format() == in.format()
                && Objects.equals(before.location(), in.location()) && Objects.equals(before.meetingUrl(), in.meetingUrl())
                && before.responsibleId().equals(in.responsibleId());
        if (unchanged) return detail(before);
        jdbc.update("""
                UPDATE appointments SET trip_id = ?, title = ?, purpose = ?, starts_at = ?, duration_minutes = ?, time_zone = ?, format = ?,
                    location = ?, meeting_url = ?, responsible_id = ?, version = version + 1, updated_at = now()
                WHERE id = ? AND agency_id = ?""",
                in.tripId(), in.title(), in.purpose(), in.startsAt(), in.durationMinutes(), in.timeZone(), in.format().name(),
                in.location(), in.meetingUrl(), in.responsibleId(), id, agency());
        AppointmentView after = find(id, false);
        Map<String, Object> detail = snapshot(after);
        if (rescheduled) {
            detail.put("previousStartsAt", before.startsAt().toString());
            detail.put("previousDurationMinutes", before.durationMinutes());
        }
        event(after, rescheduled ? "RESCHEDULED" : "UPDATED", detail);
        return detail(after);
    }

    @Transactional
    public Detail changeStatus(UUID id, StatusInput input) {
        AppointmentView before = find(id, true);
        if (input == null) invalid("Informe o novo estado.");
        if (before.status() == input.status()) return detail(before);
        expectVersion(before.version(), input.expectedVersion());
        Agenda.requireTransition(before.status(), input.status(), input.reason());
        if ((input.status() == Status.COMPLETED || input.status() == Status.NO_SHOW) && before.startsAt().toInstant().isAfter(Instant.now())) {
            invalid("Só é possível marcar como realizado ou não compareceu depois do horário de início.");
        }
        if (input.status() == Status.NO_SHOW && before.minutesVersion() != null) {
            invalid("Este compromisso já tem ata. Marque como realizado ou corrija a ata.");
        }
        String reason = input.reason() == null || input.reason().isBlank() ? null : input.reason().strip();
        jdbc.update("UPDATE appointments SET status = ?, status_reason = ?, status_changed_at = now(), version = version + 1, updated_at = now() WHERE id = ? AND agency_id = ?",
                input.status().name(), reason, id, agency());
        AppointmentView after = find(id, false);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("from", before.status().name());
        detail.put("to", after.status().name());
        detail.put("reason", reason);
        event(after, before.status() == Status.SCHEDULED ? "STATUS_" + after.status().name() : "REOPENED", detail);
        return detail(after);
    }

    /** Exclusão definitiva só de compromisso agendado sem ata (cadastro por engano); demais casos usam cancelamento. */
    @Transactional
    public void delete(UUID id, Integer expectedVersion) {
        AppointmentView before = find(id, true);
        expectVersion(before.version(), expectedVersion);
        if (before.status() != Status.SCHEDULED || before.minutesVersion() != null) {
            throw new AgendaConflictException("Compromissos realizados, cancelados ou com ata não são excluídos. Use o cancelamento para preservar o histórico.");
        }
        jdbc.update("DELETE FROM appointments WHERE id = ? AND agency_id = ?", id, agency());
        event(before, "DELETED", snapshot(before));
    }

    /** Cria (versão esperada 0) ou edita a ata; cada alteração grava uma versão imutável. */
    @Transactional
    public Detail saveMinutes(UUID id, MinutesInput input) {
        AppointmentView appointment = find(id, true);
        if (!Agenda.minutesAllowed(appointment.status())) {
            throw new AgendaConflictException("Atas são registradas em compromissos agendados ou realizados. Reabra o compromisso se necessário.");
        }
        if (input == null) invalid("Informe o conteúdo da ata.");
        Minutes data = Agenda.normalize(input.data());
        int current = appointment.minutesVersion() == null ? 0 : appointment.minutesVersion();
        expectVersion(current, input.expectedVersion());
        if (current > 0) {
            Minutes stored = jdbc.queryForObject("SELECT data FROM appointment_minutes WHERE appointment_id = ? AND agency_id = ?",
                    (rs, n) -> read(rs.getString(1), Minutes.class), id, agency());
            if (data.equals(stored)) return detail(appointment);
        }
        int version = current + 1;
        String json = json(data);
        if (current == 0) {
            jdbc.update("INSERT INTO appointment_minutes(appointment_id, agency_id, data, search_text) VALUES (?, ?, ?::jsonb, ?)",
                    id, agency(), json, Agenda.searchText(data));
        } else {
            jdbc.update("UPDATE appointment_minutes SET data = ?::jsonb, search_text = ?, version = ?, updated_at = now() WHERE appointment_id = ? AND agency_id = ?",
                    json, Agenda.searchText(data), version, id, agency());
        }
        jdbc.update("INSERT INTO appointment_minutes_versions(id, appointment_id, agency_id, version, data, author) VALUES (?, ?, ?, ?, ?::jsonb, ?)",
                UUID.randomUUID(), id, agency(), version, json, author());
        event(appointment, current == 0 ? "MINUTES_CREATED" : "MINUTES_UPDATED",
                Map.of("minutesVersion", version, "decisions", data.decisions().size(), "nextSteps", data.nextSteps().size()));
        return detail(find(id, false));
    }

    // ------------------------------------------------------------------ apoio

    private void validateReferences(AppointmentInput in) {
        Boolean responsible = jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM users WHERE id = ? AND agency_id = ? AND active = true AND account_kind IN ('AGENCY_OWNER','SALES_AGENT'))""",
                Boolean.class, in.responsibleId(), agency());
        if (!Boolean.TRUE.equals(responsible)) throw new ResourceNotFoundException("Responsável não encontrado na agência.");
        if (in.tripId() != null) {
            Boolean trip = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM trips WHERE id = ? AND agency_id = ? AND customer_id = ?)",
                    Boolean.class, in.tripId(), agency(), in.customerId());
            if (!Boolean.TRUE.equals(trip)) throw new ResourceNotFoundException("Viagem não encontrada para esta pessoa.");
        }
    }

    private void lockCustomer(UUID customerId) {
        List<UUID> found = jdbc.query("SELECT id FROM customers WHERE id = ? AND agency_id = ? FOR UPDATE",
                (rs, n) -> rs.getObject(1, UUID.class), customerId, agency());
        if (found.isEmpty()) throw new ResourceNotFoundException("Pessoa não encontrada.");
    }

    private AppointmentView find(UUID id, boolean lock) {
        if (lock) {
            List<UUID> locked = jdbc.query("SELECT id FROM appointments WHERE id = ? AND agency_id = ? FOR UPDATE",
                    (rs, n) -> rs.getObject(1, UUID.class), id, agency());
            if (locked.isEmpty()) throw notFound();
        }
        return jdbc.query(SELECT + " AND a.id = ?", ROW, agency(), id).stream().findFirst().orElseThrow(AgendaService::notFound);
    }

    private Detail detail(AppointmentView a) {
        MinutesView minutes = jdbc.query("SELECT data, version, updated_at FROM appointment_minutes WHERE appointment_id = ? AND agency_id = ?",
                (rs, n) -> new MinutesView(read(rs.getString(1), Minutes.class), rs.getInt(2), rs.getObject(3, OffsetDateTime.class)),
                a.id(), agency()).stream().findFirst().orElse(null);
        List<VersionMeta> versions = jdbc.query("SELECT version, author, created_at FROM appointment_minutes_versions WHERE appointment_id = ? AND agency_id = ? ORDER BY version DESC",
                (rs, n) -> new VersionMeta(rs.getInt(1), rs.getString(2), rs.getObject(3, OffsetDateTime.class)), a.id(), agency());
        List<Overlap> overlaps = a.status() != Status.SCHEDULED ? List.of() : jdbc.query("""
                SELECT id, title, starts_at, duration_minutes FROM appointments
                WHERE agency_id = ? AND responsible_id = ? AND id <> ? AND status = 'SCHEDULED'
                  AND starts_at < ? AND starts_at + make_interval(mins => duration_minutes) > ?
                ORDER BY starts_at""",
                (rs, n) -> new Overlap(rs.getObject(1, UUID.class), rs.getString(2), rs.getObject(3, OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC), rs.getInt(4)),
                agency(), a.responsibleId(), a.id(), a.endsAt(), a.startsAt());
        List<Event> history = jdbc.query("SELECT id, action, detail, author, created_at FROM appointment_events WHERE agency_id = ? AND appointment_id = ? ORDER BY created_at DESC, id",
                (rs, n) -> new Event(rs.getObject(1, UUID.class), rs.getString(2), read(rs.getString(3), Object.class), rs.getString(4), rs.getObject(5, OffsetDateTime.class)),
                agency(), a.id());
        return new Detail(a, minutes, versions, overlaps, history);
    }

    private Map<String, Object> snapshot(AppointmentView a) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("product", a.product().name());
        detail.put("title", a.title());
        detail.put("startsAt", a.startsAt().toString());
        detail.put("durationMinutes", a.durationMinutes());
        detail.put("timeZone", a.timeZone());
        detail.put("responsibleId", a.responsibleId());
        detail.put("status", a.status().name());
        return detail;
    }

    private void event(AppointmentView a, String action, Object detail) {
        jdbc.update("INSERT INTO appointment_events(id, agency_id, appointment_id, customer_id, action, detail, author) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)",
                UUID.randomUUID(), agency(), a.id(), a.customerId(), action, json(detail), author());
    }

    private void expectVersion(int actual, Integer expected) {
        if (expected == null || actual != expected) {
            throw new AgendaConflictException("O compromisso foi alterado por outra pessoa. Recarregue antes de continuar.");
        }
    }

    private static UUID agency() { return TenantContext.requireAgencyId(); }

    private static String author() {
        PlatformAccount user = SecurityContextUsers.requireUser();
        return user.getName() + " <" + user.getEmail() + ">";
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível registrar o compromisso.", e);
        }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return value == null ? null : mapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível consultar o compromisso.", e);
        }
    }

    private static ResourceNotFoundException notFound() { return new ResourceNotFoundException("Compromisso não encontrado."); }
}
