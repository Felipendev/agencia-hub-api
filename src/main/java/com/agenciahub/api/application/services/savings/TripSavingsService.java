package com.agenciahub.api.application.services.savings;

import com.agenciahub.api.application.services.savings.SavingsSupport.AuditEntry;
import com.agenciahub.api.domain.savings.TripSavings;
import com.agenciahub.api.domain.savings.TripSavings.Data;
import com.agenciahub.api.domain.savings.TripSavings.Result;
import com.agenciahub.api.exception.ResourceNotFoundException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Análises de economia por viagem: edição pelo mesmo ID com versões, evidências e auditoria.
 * Somente leitura sobre viagens, vendas e financeiro; nada é criado nesses módulos.
 */
@Service
@RequiredArgsConstructor
public class TripSavingsService {

    static final String SAVINGS = "SAVINGS";
    static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    static final Set<String> FILE_TYPES = Set.of("application/pdf", "image/jpeg", "image/png", "image/webp");

    private final JdbcTemplate jdbc;
    private final SavingsSupport support;

    public record TripRef(UUID tripId, UUID customerId, String customerName, String serviceType, String bookingLocator,
                          String airline, String status, LocalDate travelStartDate, LocalDate travelEndDate) {
        public boolean cancelled() { return "CANCELLED".equalsIgnoreCase(status); }
    }

    public record SavingsRow(TripRef trip, UUID savingsId, int version, Data data, Result result, OffsetDateTime updatedAt) {}

    public record VersionMeta(int version, String author, OffsetDateTime createdAt) {}

    public record VersionView(int version, Data data, Result result, String author, OffsetDateTime createdAt) {}

    public record EvidenceMeta(UUID id, String description, String filename, String contentType, long byteSize,
                               String author, OffsetDateTime createdAt) {}

    public record SavingsView(TripRef trip, UUID savingsId, int version, Data data, Result result, List<VersionMeta> versions,
                              List<EvidenceMeta> evidence, List<AuditEntry> history) {}

    public record SaveInput(@NotNull Data data, @NotNull @Min(0) Integer expectedVersion) {}

    public record FileContent(String filename, String contentType, byte[] bytes) {}

    public record Totals(int trips, int available, int unavailable, int cancelled, BigDecimal comparablePrice,
                         BigDecimal effectiveCost, BigDecimal savings, BigDecimal savingsPercent) {}

    public record Report(UUID customerId, LocalDate from, LocalDate to, List<SavingsRow> rows, Totals totals) {}

    private static final String ROW_SQL = """
            SELECT t.id AS trip_id, t.customer_id, p.name AS customer_name, t.service_type, t.booking_locator, t.airline,
                   t.status, t.travel_start_date, t.travel_end_date,
                   s.id AS savings_id, s.version, s.data, s.updated_at
            FROM trips t
            JOIN customers p ON p.id = t.customer_id AND p.agency_id = t.agency_id
            LEFT JOIN trip_savings s ON s.trip_id = t.id AND s.agency_id = t.agency_id
            WHERE t.agency_id = ?""";

    private record Joined(TripRef trip, UUID savingsId, int version, Data data, OffsetDateTime updatedAt) {
        Result result() { return data == null ? Result.noAnalysis(trip.cancelled()) : TripSavings.compute(data, trip.cancelled()); }

        SavingsRow row() { return new SavingsRow(trip, savingsId, version, data, result(), updatedAt); }
    }

    private RowMapper<Joined> joined() {
        return (rs, n) -> new Joined(
                new TripRef(rs.getObject("trip_id", UUID.class), rs.getObject("customer_id", UUID.class), rs.getString("customer_name"),
                        rs.getString("service_type"), rs.getString("booking_locator"), rs.getString("airline"), rs.getString("status"),
                        rs.getObject("travel_start_date", LocalDate.class), rs.getObject("travel_end_date", LocalDate.class)),
                rs.getObject("savings_id", UUID.class), rs.getInt("version"),
                support.read(rs.getString("data"), Data.class), rs.getObject("updated_at", OffsetDateTime.class));
    }

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public List<SavingsRow> list(UUID customerId, LocalDate from, LocalDate to, Boolean available) {
        return query(customerId, from, to).stream().map(Joined::row)
                .filter(row -> available == null || row.result().available() == available).toList();
    }

    @Transactional(readOnly = true)
    public Report report(UUID customerId, LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) TripSavings.invalid("O fim do período não pode ser anterior ao início.");
        List<SavingsRow> rows = query(customerId, from, to).stream().map(Joined::row).toList();
        return new Report(customerId, from, to, rows, totals(rows));
    }

    static Totals totals(List<SavingsRow> rows) {
        List<Result> available = rows.stream().map(SavingsRow::result).filter(Result::available).toList();
        BigDecimal price = available.stream().map(Result::comparablePrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cost = available.stream().map(Result::effectiveCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal savings = available.stream().map(Result::savings).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal percent = price.signum() == 0 ? null : savings.multiply(BigDecimal.valueOf(100)).divide(price, 1, RoundingMode.HALF_UP);
        int cancelled = (int) rows.stream().filter(r -> r.trip().cancelled()).count();
        return new Totals(rows.size(), available.size(), rows.size() - available.size(), cancelled,
                available.isEmpty() ? null : price, available.isEmpty() ? null : cost, available.isEmpty() ? null : savings, percent);
    }

    @Transactional(readOnly = true)
    public SavingsView view(UUID tripId) {
        return view(joined(tripId, false));
    }

    @Transactional(readOnly = true)
    public VersionView version(UUID tripId, int version) {
        Joined current = joined(tripId, false);
        if (current.savingsId() == null) throw new ResourceNotFoundException("Análise de economia não encontrada.");
        return jdbc.query("SELECT version, data, author, created_at FROM trip_savings_versions WHERE savings_id = ? AND agency_id = ? AND version = ?",
                        (rs, n) -> {
                            Data data = support.read(rs.getString("data"), Data.class);
                            return new VersionView(rs.getInt("version"), data, TripSavings.compute(data, current.trip().cancelled()),
                                    rs.getString("author"), rs.getObject("created_at", OffsetDateTime.class));
                        }, current.savingsId(), support.agency(), version).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Versão da análise não encontrada."));
    }

    @Transactional(readOnly = true)
    public FileContent evidence(UUID tripId, UUID evidenceId) {
        Joined current = joined(tripId, false);
        return jdbc.query("SELECT filename, content_type, content FROM trip_savings_evidence WHERE id = ? AND savings_id = ? AND agency_id = ?",
                        (rs, n) -> new FileContent(rs.getString(1), rs.getString(2), rs.getBytes(3)),
                        evidenceId, current.savingsId(), support.agency()).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Evidência não encontrada."));
    }

    // ------------------------------------------------------------------ escrita

    /** Cria (versão esperada 0) ou edita pelo mesmo ID; cada alteração grava uma versão imutável. */
    @Transactional
    public SavingsView save(UUID tripId, SaveInput input) {
        Joined before = joined(tripId, true);
        Data data = normalize(input.data());
        TripSavings.validate(data);
        support.expectVersion(before.version(), input.expectedVersion());
        if (before.data() != null && before.data().equals(data)) return view(before);
        String author = support.author();
        UUID savingsId = before.savingsId();
        int version;
        if (savingsId == null) {
            savingsId = UUID.randomUUID();
            version = 1;
            jdbc.update("INSERT INTO trip_savings(id, agency_id, trip_id, data, created_by) VALUES (?, ?, ?, ?::jsonb, ?)",
                    savingsId, support.agency(), tripId, support.json(data), author);
        } else {
            version = before.version() + 1;
            jdbc.update("UPDATE trip_savings SET data = ?::jsonb, version = ?, updated_at = now() WHERE id = ? AND agency_id = ?",
                    support.json(data), version, savingsId, support.agency());
        }
        jdbc.update("INSERT INTO trip_savings_versions(id, savings_id, agency_id, version, data, author) VALUES (?, ?, ?, ?, ?::jsonb, ?)",
                UUID.randomUUID(), savingsId, support.agency(), version, support.json(data), author);
        Result result = TripSavings.compute(data, before.trip().cancelled());
        support.audit(SAVINGS, savingsId, tripId, before.trip().customerId(), version == 1 ? "SAVINGS_CREATED" : "SAVINGS_UPDATED",
                resultDetail(version, result));
        return view(joined(tripId, false));
    }

    @Transactional
    public void delete(UUID tripId, Integer expectedVersion) {
        Joined before = joined(tripId, true);
        if (before.savingsId() == null) throw new ResourceNotFoundException("Análise de economia não encontrada.");
        support.expectVersion(before.version(), expectedVersion);
        jdbc.update("DELETE FROM trip_savings WHERE id = ? AND agency_id = ?", before.savingsId(), support.agency());
        support.audit(SAVINGS, before.savingsId(), tripId, before.trip().customerId(), "SAVINGS_DELETED",
                resultDetail(before.version(), before.result()));
    }

    @Transactional
    public EvidenceMeta addEvidence(UUID tripId, String description, MultipartFile file) {
        Joined current = joined(tripId, true);
        if (current.savingsId() == null) TripSavings.invalid("Salve a análise antes de anexar evidências.");
        if (file == null || file.isEmpty()) TripSavings.invalid("Escolha um arquivo.");
        if (file.getSize() > MAX_FILE_BYTES) TripSavings.invalid("O arquivo pode ter até 5 MB.");
        String type = Objects.toString(file.getContentType(), "");
        if (!FILE_TYPES.contains(type)) TripSavings.invalid("Envie PDF ou imagem (JPG, PNG, WEBP).");
        if (description != null && description.length() > 500) TripSavings.invalid("A descrição pode ter até 500 caracteres.");
        UUID id = UUID.randomUUID();
        String filename = filename(file);
        try {
            jdbc.update("""
                    INSERT INTO trip_savings_evidence(id, savings_id, agency_id, description, filename, content_type, byte_size, content, author)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    id, current.savingsId(), support.agency(), description == null || description.isBlank() ? null : description.strip(),
                    filename, type, file.getSize(), file.getBytes(), support.author());
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler o arquivo enviado.", e);
        }
        support.audit(SAVINGS, current.savingsId(), tripId, current.trip().customerId(), "EVIDENCE_ADDED", Map.of("evidenceId", id, "filename", filename));
        return evidenceList(current.savingsId()).stream().filter(e -> e.id().equals(id)).findFirst().orElseThrow();
    }

    @Transactional
    public void deleteEvidence(UUID tripId, UUID evidenceId) {
        Joined current = joined(tripId, true);
        EvidenceMeta evidence = current.savingsId() == null ? null
                : evidenceList(current.savingsId()).stream().filter(e -> e.id().equals(evidenceId)).findFirst().orElse(null);
        if (evidence == null) throw new ResourceNotFoundException("Evidência não encontrada.");
        jdbc.update("DELETE FROM trip_savings_evidence WHERE id = ? AND savings_id = ? AND agency_id = ?", evidenceId, current.savingsId(), support.agency());
        support.audit(SAVINGS, current.savingsId(), tripId, current.trip().customerId(), "EVIDENCE_DELETED",
                Map.of("evidenceId", evidenceId, "filename", evidence.filename()));
    }

    // ------------------------------------------------------------------ apoio

    private List<Joined> query(UUID customerId, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder(ROW_SQL);
        List<Object> args = new ArrayList<>(List.of(support.agency()));
        if (customerId != null) { sql.append(" AND t.customer_id = ?"); args.add(customerId); }
        // Com filtro de período, viagem sem data de partida não pertence ao resultado (mesma regra da lista de viagens).
        if (from != null) { sql.append(" AND t.travel_start_date >= ?"); args.add(from); }
        if (to != null) { sql.append(" AND t.travel_start_date <= ?"); args.add(to); }
        sql.append(" ORDER BY t.travel_start_date NULLS LAST, t.id");
        return jdbc.query(sql.toString(), joined(), args.toArray());
    }

    private Joined joined(UUID tripId, boolean lock) {
        if (lock) {
            // Serializa criação/edição da análise da mesma viagem.
            List<UUID> locked = jdbc.query("SELECT id FROM trips WHERE id = ? AND agency_id = ? FOR UPDATE",
                    (rs, n) -> rs.getObject(1, UUID.class), tripId, support.agency());
            if (locked.isEmpty()) throw notFound();
        }
        return jdbc.query(ROW_SQL + " AND t.id = ?", joined(), support.agency(), tripId).stream().findFirst()
                .orElseThrow(TripSavingsService::notFound);
    }

    private SavingsView view(Joined joined) {
        List<VersionMeta> versions = joined.savingsId() == null ? List.of() : jdbc.query("""
                SELECT version, author, created_at FROM trip_savings_versions WHERE savings_id = ? AND agency_id = ? ORDER BY version DESC""",
                (rs, n) -> new VersionMeta(rs.getInt(1), rs.getString(2), rs.getObject(3, OffsetDateTime.class)),
                joined.savingsId(), support.agency());
        List<EvidenceMeta> evidence = joined.savingsId() == null ? List.of() : evidenceList(joined.savingsId());
        return new SavingsView(joined.trip(), joined.savingsId(), joined.version(), joined.data(), joined.result(), versions, evidence,
                support.history(joined.trip().tripId()));
    }

    private List<EvidenceMeta> evidenceList(UUID savingsId) {
        return jdbc.query("""
                SELECT id, description, filename, content_type, byte_size, author, created_at FROM trip_savings_evidence
                WHERE savings_id = ? AND agency_id = ? ORDER BY created_at, id""",
                (rs, n) -> new EvidenceMeta(rs.getObject("id", UUID.class), rs.getString("description"), rs.getString("filename"),
                        rs.getString("content_type"), rs.getLong("byte_size"), rs.getString("author"), rs.getObject("created_at", OffsetDateTime.class)),
                savingsId, support.agency());
    }

    /** Textos em branco viram {@code null}, para que salvar sem mudanças não gere versão. */
    static Data normalize(Data data) {
        if (data == null) return null;
        TripSavings.Comparison c = data.comparison();
        TripSavings.Comparison comparison = c == null ? null : new TripSavings.Comparison(money(c.price()), c.sourceType(), text(c.sourceDescription()),
                text(c.sourceUrl()), c.researchedOn(), text(c.route()), c.outboundDate(), c.returnDate(), c.passengers(),
                text(c.fareConditions()), text(c.baggage()), c.sameRoute(), c.sameDates(), c.samePassengers(), c.equivalentConditions(),
                text(c.differences()));
        List<TripSavings.CostLine> costs = data.costs().stream().map(l -> l == null ? null
                : new TripSavings.CostLine(l.kind(), money(l.amount()), text(l.description()), text(l.origin()), l.miles())).toList();
        List<TripSavings.Adjustment> adjustments = data.adjustments().stream().map(a -> a == null ? null
                : new TripSavings.Adjustment(a.kind(), money(a.amount()), a.date(), text(a.description()))).toList();
        return new Data(comparison, costs, data.milesUsage(), adjustments, text(data.methodology()), text(data.notes()));
    }

    /** Escala 2 para comparar versões; valores com mais casas seguem para a validação recusar. */
    private static BigDecimal money(BigDecimal value) {
        return value == null || value.scale() > 2 ? value : value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static String text(String value) { return value == null || value.isBlank() ? null : value.strip(); }

    private static Map<String, Object> resultDetail(int version, Result result) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("version", version);
        detail.put("available", result.available());
        detail.put("savings", result.savings());
        detail.put("reasons", result.reasons().stream().map(r -> r.code().name()).toList());
        return detail;
    }

    private static String filename(MultipartFile file) {
        String name = Objects.toString(file.getOriginalFilename(), "arquivo").replaceAll("[\\r\\n\\\\/]", "_").strip();
        if (name.isEmpty()) name = "arquivo";
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private static ResourceNotFoundException notFound() { return new ResourceNotFoundException("Viagem não encontrada."); }
}
