package com.agenciahub.api.application.services.savings;

import com.agenciahub.api.application.services.savings.TripSavingsService.Report;
import com.agenciahub.api.application.services.savings.TripSavingsService.SavingsRow;
import com.agenciahub.api.application.services.savings.TripSavingsService.Totals;
import com.agenciahub.api.domain.savings.TripSavings;
import com.agenciahub.api.domain.savings.TripSavings.Data;
import com.agenciahub.api.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Relatório compartilhável: o gestor revisa a prévia, escolhe o que ocultar e gera um retrato imutável.
 * Não há link público; o compartilhamento é feito pelo PDF.
 */
@Service
@RequiredArgsConstructor
public class SavingsReportService {

    static final String REPORT = "REPORT";

    private final JdbcTemplate jdbc;
    private final SavingsSupport support;
    private final TripSavingsService savings;
    private final SavingsReportPdf pdf;

    public record Options(boolean hideCustomerName, boolean hideLocators, boolean hideSources, boolean hideNotes,
                          @Size(max = 200) String title, @Size(max = 2000) String message) {}

    public record ReportInput(@NotNull UUID customerId, LocalDate from, LocalDate to, @NotNull @Valid Options options) {}

    public record Cost(String kind, BigDecimal amount, String description, String origin) {}

    public record Adjustment(String kind, BigDecimal amount, LocalDate date, String description) {}

    public record Line(String service, String airline, String locator, String status, LocalDate travelStartDate, LocalDate travelEndDate,
                       boolean available, List<String> unavailableReasons, BigDecimal comparablePrice, String sourceType,
                       String source, LocalDate researchedOn, String route, Integer passengers, String fareConditions,
                       String baggage, String differences, String milesUsage, List<Cost> costs, List<Adjustment> adjustments,
                       BigDecimal effectiveCost, BigDecimal savings, BigDecimal savingsPercent, String methodology, String notes) {}

    public record Content(String title, String customerLabel, LocalDate from, LocalDate to, String message, List<Line> lines,
                          Totals totals, LocalDate generatedOn) {}

    public record SharedReport(UUID id, UUID customerId, LocalDate from, LocalDate to, Options options, Content content,
                               String author, OffsetDateTime createdAt) {}

    public record SharedReportSummary(UUID id, UUID customerId, String title, LocalDate from, LocalDate to, int trips,
                                      BigDecimal savings, String author, OffsetDateTime createdAt) {}

    /** Prévia exatamente igual ao que será gravado, sem persistir nada. */
    @Transactional(readOnly = true)
    public Content preview(ReportInput input) {
        String customerName = customerName(input.customerId());
        Report report = savings.report(input.customerId(), input.from(), input.to());
        return content(report, customerName, input.options());
    }

    @Transactional
    public SharedReport create(ReportInput input) {
        Content content = preview(input);
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO trip_savings_reports(id, agency_id, customer_id, period_from, period_to, options, content, author)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?)""",
                id, support.agency(), input.customerId(), input.from(), input.to(), support.json(input.options()),
                support.json(content), support.author());
        support.audit(REPORT, id, null, input.customerId(), "REPORT_GENERATED",
                java.util.Map.of("trips", content.totals().trips(), "hideCustomerName", input.options().hideCustomerName(),
                        "hideLocators", input.options().hideLocators(), "hideSources", input.options().hideSources(),
                        "hideNotes", input.options().hideNotes()));
        return get(id);
    }

    @Transactional(readOnly = true)
    public List<SharedReportSummary> list(UUID customerId) {
        String sql = "SELECT * FROM trip_savings_reports WHERE agency_id = ?" + (customerId == null ? "" : " AND customer_id = ?")
                + " ORDER BY created_at DESC, id";
        Object[] args = customerId == null ? new Object[]{support.agency()} : new Object[]{support.agency(), customerId};
        return jdbc.query(sql, (rs, n) -> {
            Content content = support.read(rs.getString("content"), Content.class);
            return new SharedReportSummary(rs.getObject("id", UUID.class), rs.getObject("customer_id", UUID.class), content.title(),
                    rs.getObject("period_from", LocalDate.class), rs.getObject("period_to", LocalDate.class), content.totals().trips(),
                    content.totals().savings(), rs.getString("author"), rs.getObject("created_at", OffsetDateTime.class));
        }, args);
    }

    @Transactional(readOnly = true)
    public SharedReport get(UUID id) {
        return jdbc.query("SELECT * FROM trip_savings_reports WHERE id = ? AND agency_id = ?", (rs, n) -> new SharedReport(
                        rs.getObject("id", UUID.class), rs.getObject("customer_id", UUID.class),
                        rs.getObject("period_from", LocalDate.class), rs.getObject("period_to", LocalDate.class),
                        support.read(rs.getString("options"), Options.class), support.read(rs.getString("content"), Content.class),
                        rs.getString("author"), rs.getObject("created_at", OffsetDateTime.class)), id, support.agency())
                .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Relatório não encontrado."));
    }

    @Transactional(readOnly = true)
    public TripSavingsService.FileContent pdf(UUID id) {
        SharedReport report = get(id);
        return new TripSavingsService.FileContent("relatorio-economia-" + report.createdAt().toLocalDate() + ".pdf", "application/pdf",
                pdf.render(report.content()));
    }

    @Transactional
    public void delete(UUID id) {
        SharedReport report = get(id);
        jdbc.update("DELETE FROM trip_savings_reports WHERE id = ? AND agency_id = ?", id, support.agency());
        support.audit(REPORT, id, null, report.customerId(), "REPORT_DELETED", java.util.Map.of("title", report.content().title()));
    }

    // ------------------------------------------------------------------ montagem

    private String customerName(UUID customerId) {
        return jdbc.query("SELECT name FROM customers WHERE id = ? AND agency_id = ?", (rs, n) -> rs.getString(1), customerId, support.agency())
                .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada."));
    }

    static Content content(Report report, String customerName, Options options) {
        String title = options.title() == null || options.title().isBlank() ? "Relatório de economia em viagens" : options.title().strip();
        String customer = options.hideCustomerName() ? "Cliente" : customerName;
        List<Line> lines = report.rows().stream().map(row -> line(row, options)).toList();
        String message = options.message() == null || options.message().isBlank() ? null : options.message().strip();
        return new Content(title, customer, report.from(), report.to(), message, lines, report.totals(), TripSavings.today());
    }

    private static Line line(SavingsRow row, Options options) {
        var trip = row.trip();
        var result = row.result();
        // O retrato guarda os dados documentados usados no cálculo, depois das ocultações.
        Data data = row.data();
        TripSavings.Comparison c = data == null ? null : data.comparison();
        String source = null;
        if (c != null && !options.hideSources()) {
            source = c.sourceUrl() == null ? c.sourceDescription() : c.sourceDescription() + " — " + c.sourceUrl();
        }
        List<Cost> costs = data == null ? List.of() : data.costs().stream()
                .map(l -> new Cost(l.kind().name(), l.amount(), l.description(), options.hideSources() ? null : l.origin())).toList();
        List<Adjustment> adjustments = data == null ? List.of() : data.adjustments().stream()
                .map(a -> new Adjustment(a.kind().name(), a.amount(), a.date(), a.description())).toList();
        return new Line(trip.serviceType(), trip.airline(), options.hideLocators() ? null : trip.bookingLocator(), trip.status(),
                trip.travelStartDate(), trip.travelEndDate(), result.available(),
                result.reasons().stream().map(TripSavings.Reason::label).toList(),
                result.comparablePrice(), c == null || c.sourceType() == null ? null : c.sourceType().name(), source,
                c == null ? null : c.researchedOn(), c == null ? null : c.route(), c == null ? null : c.passengers(),
                c == null ? null : c.fareConditions(), c == null ? null : c.baggage(), c == null ? null : c.differences(),
                data == null || data.milesUsage() == null ? null : data.milesUsage().name(), costs, adjustments,
                result.effectiveCost(), result.savings(), result.savingsPercent(),
                data == null ? null : data.methodology(), data == null || options.hideNotes() ? null : data.notes());
    }
}
