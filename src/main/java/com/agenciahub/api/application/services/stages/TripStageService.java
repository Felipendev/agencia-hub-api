package com.agenciahub.api.application.services.stages;

import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.domain.stages.TripStages;
import com.agenciahub.api.domain.stages.TripStages.*;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.exception.TripStageConflictException;
import com.agenciahub.api.security.SecurityContextUsers;
import com.agenciahub.api.security.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static com.agenciahub.api.domain.stages.TripStages.invalid;

/**
 * Etapas da viagem com vínculos (sem cópia) a anexos da própria viagem e à Biblioteca.
 * Não cria despesas, vendas, trechos aéreos nem anexos.
 */
@Service
@RequiredArgsConstructor
public class TripStageService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public record TripRef(UUID id, UUID customerId, String customerName, String serviceType, String bookingLocator, String status,
                          LocalDate travelStartDate, LocalDate travelEndDate) {
        boolean cancelled() { return "CANCELLED".equalsIgnoreCase(status); }
    }

    public record Link(UUID id, String type, UUID targetId, String title, String detail, String author, OffsetDateTime createdAt) {}

    public record Stage(UUID id, Kind kind, String title, OffsetDateTime startsAt, OffsetDateTime endsAt, String timeZone,
                        String provider, String confirmationCode, String location, String origin, String destination,
                        Integer people, String notes, Status status, String statusReason, int version,
                        List<Link> links, List<String> warnings, OffsetDateTime updatedAt) {}

    public record Event(UUID id, UUID stageId, String action, Object detail, String author, OffsetDateTime createdAt) {}

    public record Itinerary(TripRef trip, List<Stage> stages, List<Event> history) {}

    public record StageDetail(TripRef trip, Stage stage, List<Event> history) {}

    private static final String STAGE_SQL = "SELECT * FROM trip_stages WHERE agency_id = ? AND trip_id = ?";

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public Itinerary itinerary(UUID tripId) {
        TripRef trip = trip(tripId, false);
        List<Stage> stages = jdbc.query(STAGE_SQL + " ORDER BY starts_at, id", rowMapper(trip), agency(), tripId).stream()
                .map(this::withLinks).toList();
        return new Itinerary(trip, stages, events(tripId, null));
    }

    @Transactional(readOnly = true)
    public StageDetail detail(UUID tripId, UUID id) {
        TripRef trip = trip(tripId, false);
        return new StageDetail(trip, stage(trip, id, false), events(tripId, id));
    }

    // ------------------------------------------------------------------ escrita

    @Transactional
    public StageDetail create(UUID tripId, UUID requestKey, StageInput raw) {
        StageInput in = TripStages.normalize(raw);
        TripRef trip = trip(tripId, true);
        Optional<Stage> existing = jdbc.query(STAGE_SQL + " AND id = ?", rowMapper(trip), agency(), tripId, requestKey).stream().findFirst();
        if (existing.isPresent()) {
            Stage s = existing.get();
            if (s.kind() != in.kind() || !s.title().equals(in.title()) || !s.startsAt().toInstant().equals(in.startsAt().toInstant())) {
                throw new TripStageConflictException("A confirmação já foi utilizada para outra etapa.");
            }
            return new StageDetail(trip, withLinks(s), events(tripId, s.id()));
        }
        Boolean used = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM trip_stage_events WHERE agency_id = ? AND stage_id = ?) OR EXISTS(SELECT 1 FROM trip_stages WHERE id = ?)",
                Boolean.class, agency(), requestKey, requestKey);
        if (Boolean.TRUE.equals(used)) throw new TripStageConflictException("Esta confirmação já foi usada. Recarregue a página antes de criar outra etapa.");
        if (trip.cancelled()) throw new TripStageConflictException("A viagem está cancelada. Reative a viagem para incluir novas etapas.");
        jdbc.update("""
                INSERT INTO trip_stages(id, agency_id, trip_id, kind, title, starts_at, ends_at, time_zone, provider, confirmation_code,
                    location, origin, destination, people, notes, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                requestKey, agency(), tripId, in.kind().name(), in.title(), in.startsAt(), in.endsAt(), in.timeZone(), in.provider(),
                in.confirmationCode(), in.location(), in.origin(), in.destination(), in.people(), in.notes(), author());
        Stage created = stage(trip, requestKey, false);
        event(tripId, created.id(), "CREATED", snapshot(created));
        return new StageDetail(trip, created, events(tripId, created.id()));
    }

    /** Edita pelo mesmo ID; mudança de horário registra o valor anterior. */
    @Transactional
    public StageDetail update(UUID tripId, UUID id, StageInput raw) {
        TripRef trip = trip(tripId, false);
        Stage before = stage(trip, id, true);
        expectVersion(before.version(), raw == null ? null : raw.expectedVersion());
        if (before.status() == Status.CANCELLED) throw new TripStageConflictException("Reative a etapa antes de editá-la.");
        StageInput in = TripStages.normalize(raw);
        boolean rescheduled = !before.startsAt().toInstant().equals(in.startsAt().toInstant())
                || !Objects.equals(before.endsAt() == null ? null : before.endsAt().toInstant(), in.endsAt() == null ? null : in.endsAt().toInstant())
                || !before.timeZone().equals(in.timeZone());
        boolean unchanged = !rescheduled && before.kind() == in.kind() && before.title().equals(in.title())
                && Objects.equals(before.provider(), in.provider()) && Objects.equals(before.confirmationCode(), in.confirmationCode())
                && Objects.equals(before.location(), in.location()) && Objects.equals(before.origin(), in.origin())
                && Objects.equals(before.destination(), in.destination()) && Objects.equals(before.people(), in.people())
                && Objects.equals(before.notes(), in.notes());
        if (unchanged) return new StageDetail(trip, before, events(tripId, id));
        jdbc.update("""
                UPDATE trip_stages SET kind = ?, title = ?, starts_at = ?, ends_at = ?, time_zone = ?, provider = ?, confirmation_code = ?,
                    location = ?, origin = ?, destination = ?, people = ?, notes = ?, version = version + 1, updated_at = now()
                WHERE id = ? AND agency_id = ? AND trip_id = ?""",
                in.kind().name(), in.title(), in.startsAt(), in.endsAt(), in.timeZone(), in.provider(), in.confirmationCode(),
                in.location(), in.origin(), in.destination(), in.people(), in.notes(), id, agency(), tripId);
        Stage after = stage(trip, id, false);
        Map<String, Object> detail = snapshot(after);
        if (rescheduled) {
            detail.put("previousStartsAt", before.startsAt().toString());
            detail.put("previousEndsAt", before.endsAt() == null ? null : before.endsAt().toString());
        }
        event(tripId, id, rescheduled ? "RESCHEDULED" : "UPDATED", detail);
        return new StageDetail(trip, after, events(tripId, id));
    }

    @Transactional
    public StageDetail changeStatus(UUID tripId, UUID id, StatusInput input) {
        TripRef trip = trip(tripId, false);
        Stage before = stage(trip, id, true);
        if (input == null) invalid("Informe a nova situação.");
        if (before.status() == input.status()) return new StageDetail(trip, before, events(tripId, id));
        expectVersion(before.version(), input.expectedVersion());
        TripStages.requireTransition(before.status(), input.status(), input.reason());
        String reason = input.reason() == null || input.reason().isBlank() ? null : input.reason().strip();
        jdbc.update("UPDATE trip_stages SET status = ?, status_reason = ?, version = version + 1, updated_at = now() WHERE id = ? AND agency_id = ? AND trip_id = ?",
                input.status().name(), reason, id, agency(), tripId);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("from", before.status().name());
        detail.put("to", input.status().name());
        detail.put("reason", reason);
        event(tripId, id, "STATUS_" + input.status().name(), detail);
        return new StageDetail(trip, stage(trip, id, false), events(tripId, id));
    }

    /** Etapas não têm efeito financeiro: a exclusão é permitida e fica na auditoria. */
    @Transactional
    public void delete(UUID tripId, UUID id, Integer expectedVersion) {
        TripRef trip = trip(tripId, false);
        Stage before = stage(trip, id, true);
        expectVersion(before.version(), expectedVersion);
        jdbc.update("DELETE FROM trip_stages WHERE id = ? AND agency_id = ? AND trip_id = ?", id, agency(), tripId);
        event(tripId, id, "DELETED", snapshot(before));
    }

    /** Vincula um anexo que já pertence a esta viagem; nada é copiado. Repetir não duplica. */
    @Transactional
    public StageDetail linkAttachment(UUID tripId, UUID id, UUID attachmentId) {
        TripRef trip = trip(tripId, false);
        stage(trip, id, true);
        Boolean belongs = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM agency_attachments WHERE id = ? AND agency_id = ? AND trip_id = ?)",
                Boolean.class, attachmentId, agency(), tripId);
        if (!Boolean.TRUE.equals(belongs)) throw new ResourceNotFoundException("Anexo não encontrado nesta viagem.");
        int inserted = jdbc.update("INSERT INTO trip_stage_links(id, stage_id, agency_id, attachment_id, author) VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
                UUID.randomUUID(), id, agency(), attachmentId, author());
        if (inserted == 1) event(tripId, id, "ATTACHMENT_LINKED", Map.of("attachmentId", attachmentId));
        return new StageDetail(trip, stage(trip, id, false), events(tripId, id));
    }

    @Transactional
    public StageDetail unlinkAttachment(UUID tripId, UUID id, UUID attachmentId) {
        TripRef trip = trip(tripId, false);
        stage(trip, id, true);
        int removed = jdbc.update("DELETE FROM trip_stage_links WHERE stage_id = ? AND agency_id = ? AND attachment_id = ?", id, agency(), attachmentId);
        if (removed == 1) event(tripId, id, "ATTACHMENT_UNLINKED", Map.of("attachmentId", attachmentId));
        return new StageDetail(trip, stage(trip, id, false), events(tripId, id));
    }

    /** Vincula um material da Biblioteca da agência; o material continua sendo gerido na Biblioteca. */
    @Transactional
    public StageDetail linkLibrary(UUID tripId, UUID id, UUID libraryId) {
        TripRef trip = trip(tripId, false);
        stage(trip, id, true);
        Boolean belongs = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM agency_library WHERE id = ? AND agency_id = ?)", Boolean.class, libraryId, agency());
        if (!Boolean.TRUE.equals(belongs)) throw new ResourceNotFoundException("Material não encontrado na Biblioteca.");
        int inserted = jdbc.update("INSERT INTO trip_stage_links(id, stage_id, agency_id, library_id, author) VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
                UUID.randomUUID(), id, agency(), libraryId, author());
        if (inserted == 1) event(tripId, id, "LIBRARY_LINKED", Map.of("libraryId", libraryId));
        return new StageDetail(trip, stage(trip, id, false), events(tripId, id));
    }

    @Transactional
    public StageDetail unlinkLibrary(UUID tripId, UUID id, UUID libraryId) {
        TripRef trip = trip(tripId, false);
        stage(trip, id, true);
        int removed = jdbc.update("DELETE FROM trip_stage_links WHERE stage_id = ? AND agency_id = ? AND library_id = ?", id, agency(), libraryId);
        if (removed == 1) event(tripId, id, "LIBRARY_UNLINKED", Map.of("libraryId", libraryId));
        return new StageDetail(trip, stage(trip, id, false), events(tripId, id));
    }

    // ------------------------------------------------------------------ apoio

    private TripRef trip(UUID tripId, boolean lock) {
        if (lock) {
            // Serializa criações na mesma viagem (idempotência e verificação de cancelamento).
            List<UUID> locked = jdbc.query("SELECT id FROM trips WHERE id = ? AND agency_id = ? FOR UPDATE", (rs, n) -> rs.getObject(1, UUID.class), tripId, agency());
            if (locked.isEmpty()) throw new ResourceNotFoundException("Viagem não encontrada.");
        }
        return jdbc.query("""
                SELECT t.id, t.customer_id, p.name, t.service_type, t.booking_locator, t.status, t.travel_start_date, t.travel_end_date
                FROM trips t JOIN customers p ON p.id = t.customer_id AND p.agency_id = t.agency_id
                WHERE t.id = ? AND t.agency_id = ?""",
                (rs, n) -> new TripRef(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getString(6), rs.getObject(7, LocalDate.class), rs.getObject(8, LocalDate.class)),
                tripId, agency()).stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada."));
    }

    private Stage stage(TripRef trip, UUID id, boolean lock) {
        if (lock) {
            List<UUID> locked = jdbc.query("SELECT id FROM trip_stages WHERE id = ? AND agency_id = ? AND trip_id = ? FOR UPDATE",
                    (rs, n) -> rs.getObject(1, UUID.class), id, agency(), trip.id());
            if (locked.isEmpty()) throw notFound();
        }
        return jdbc.query(STAGE_SQL + " AND id = ?", rowMapper(trip), agency(), trip.id(), id).stream().findFirst()
                .map(this::withLinks).orElseThrow(TripStageService::notFound);
    }

    private RowMapper<Stage> rowMapper(TripRef trip) {
        return (rs, n) -> {
            OffsetDateTime start = rs.getObject("starts_at", OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC);
            OffsetDateTime end = Optional.ofNullable(rs.getObject("ends_at", OffsetDateTime.class)).map(t -> t.withOffsetSameInstant(ZoneOffset.UTC)).orElse(null);
            String zone = rs.getString("time_zone");
            return new Stage(rs.getObject("id", UUID.class), Kind.valueOf(rs.getString("kind")), rs.getString("title"), start, end, zone,
                    rs.getString("provider"), rs.getString("confirmation_code"), rs.getString("location"), rs.getString("origin"),
                    rs.getString("destination"), (Integer) rs.getObject("people"), rs.getString("notes"), Status.valueOf(rs.getString("status")),
                    rs.getString("status_reason"), rs.getInt("version"), List.of(),
                    TripStages.warnings(start.toInstant(), end == null ? null : end.toInstant(), zone, trip.travelStartDate(), trip.travelEndDate()),
                    rs.getObject("updated_at", OffsetDateTime.class));
        };
    }

    private Stage withLinks(Stage s) {
        List<Link> links = jdbc.query("""
                SELECT l.id, l.attachment_id, l.library_id, a.original_filename, a.description, b.title AS library_title, b.kind AS library_kind,
                       l.author, l.created_at
                FROM trip_stage_links l
                LEFT JOIN agency_attachments a ON a.id = l.attachment_id AND a.agency_id = l.agency_id
                LEFT JOIN agency_library b ON b.id = l.library_id AND b.agency_id = l.agency_id
                WHERE l.stage_id = ? AND l.agency_id = ? ORDER BY l.created_at, l.id""",
                (rs, n) -> {
                    UUID attachment = rs.getObject("attachment_id", UUID.class);
                    return attachment != null
                            ? new Link(rs.getObject("id", UUID.class), "ATTACHMENT", attachment, rs.getString("original_filename"), rs.getString("description"),
                                    rs.getString("author"), rs.getObject("created_at", OffsetDateTime.class))
                            : new Link(rs.getObject("id", UUID.class), "LIBRARY", rs.getObject("library_id", UUID.class), rs.getString("library_title"),
                                    rs.getString("library_kind"), rs.getString("author"), rs.getObject("created_at", OffsetDateTime.class));
                }, s.id(), agency());
        return new Stage(s.id(), s.kind(), s.title(), s.startsAt(), s.endsAt(), s.timeZone(), s.provider(), s.confirmationCode(), s.location(),
                s.origin(), s.destination(), s.people(), s.notes(), s.status(), s.statusReason(), s.version(), links, s.warnings(), s.updatedAt());
    }

    private List<Event> events(UUID tripId, UUID stageId) {
        String sql = "SELECT id, stage_id, action, detail, author, created_at FROM trip_stage_events WHERE agency_id = ? AND trip_id = ?"
                + (stageId == null ? "" : " AND stage_id = ?") + " ORDER BY created_at DESC, id LIMIT 200";
        Object[] args = stageId == null ? new Object[]{agency(), tripId} : new Object[]{agency(), tripId, stageId};
        return jdbc.query(sql, (rs, n) -> new Event(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3),
                read(rs.getString(4)), rs.getString(5), rs.getObject(6, OffsetDateTime.class)), args);
    }

    private Map<String, Object> snapshot(Stage s) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("kind", s.kind().name());
        detail.put("title", s.title());
        detail.put("startsAt", s.startsAt().toString());
        detail.put("endsAt", s.endsAt() == null ? null : s.endsAt().toString());
        detail.put("timeZone", s.timeZone());
        detail.put("status", s.status().name());
        return detail;
    }

    private void event(UUID tripId, UUID stageId, String action, Object detail) {
        jdbc.update("INSERT INTO trip_stage_events(id, agency_id, trip_id, stage_id, action, detail, author) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)",
                UUID.randomUUID(), agency(), tripId, stageId, action, json(detail), author());
    }

    private void expectVersion(int actual, Integer expected) {
        if (expected == null || actual != expected) throw new TripStageConflictException("A etapa foi alterada por outra pessoa. Recarregue antes de continuar.");
    }

    private static UUID agency() { return TenantContext.requireAgencyId(); }

    private static String author() {
        PlatformAccount user = SecurityContextUsers.requireUser();
        return user.getName() + " <" + user.getEmail() + ">";
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Não foi possível registrar a etapa.", e); }
    }

    private Object read(String value) {
        try { return value == null ? null : mapper.readValue(value, Object.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Não foi possível consultar a etapa.", e); }
    }

    private static ResourceNotFoundException notFound() { return new ResourceNotFoundException("Etapa não encontrada."); }
}
