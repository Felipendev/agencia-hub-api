package com.agenciahub.api.application.services.history;

import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/** Read-only projection: never returns snapshots or modifies source audits. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityHistoryService {
    public enum Product { TRAVEL, MILES }
    public enum Source { WALLET, CONTRACTS, AGENDA, TASKS, SIMULATIONS, PLAN_TASKS, DIAGNOSTICS, TRIP_STAGES, SAVINGS, REFERRALS, FOLLOWUPS }
    public record Filters(Product product, UUID customerId, LocalDate from, LocalDate to, Set<Source> modules) {}
    public record Event(UUID id, Source module, String resourceId, String customerId, String action,
                        UUID authorId, String authorName, OffsetDateTime at) {}
    public record Page(List<Event> events, boolean hasMore, int page, String timeZone) {}
    public static final ZoneId ZONE = ZoneId.of("America/Fortaleza");
    private final NamedParameterJdbcTemplate jdbc;

    public Page list(Filters filters, int page) {
        if (page < 0 || page > 49) invalid("A página deve estar entre 0 e 49. Reduza o período para consultar outros eventos.");
        List<Event> rows = query(filters, 101, page * 100);
        return new Page(rows.stream().limit(100).toList(), rows.size() > 100, page, ZONE.getId());
    }

    public byte[] export(Filters filters) {
        List<Event> rows = query(filters, 5001, 0);
        if (rows.size() > 5000) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Há mais de 5.000 eventos. Reduza o período, a pessoa ou os módulos; nenhum arquivo parcial foi gerado.");
        StringBuilder csv = new StringBuilder("\uFEFFProduto;Módulo;Evento;Registro;Pessoa;Ação;Autor ID;Autor;Data (America/Fortaleza)\r\n");
        for (Event e : rows) {
            csv.append(List.of(filters.product().name(), e.module().name(), e.id().toString(), e.resourceId(),
                            Objects.toString(e.customerId(), ""), e.action(), Objects.toString(e.authorId(), ""),
                            e.authorName(), e.at().atZoneSameInstant(ZONE).toOffsetDateTime().toString())
                    .stream().map(ActivityHistoryService::cell).collect(Collectors.joining(";"))).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String cell(String value) {
        // All columns are quoted; spreadsheet formula prefixes are neutralized even after whitespace/control chars.
        if (value.matches("(?s)^[\\s\\p{Z}\\p{Cc}]*[=+@-].*")) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private List<Event> query(Filters f, int limit, int offset) {
        validate(f);
        UUID agency = TenantContext.requireAgencyId();
        var args = new MapSqlParameterSource().addValue("agency", agency)
                .addValue("product", f.product().name())
                .addValue("start", f.from().atStartOfDay(ZONE).toOffsetDateTime())
                .addValue("end", f.to().plusDays(1).atStartOfDay(ZONE).toOffsetDateTime())
                .addValue("limit", limit).addValue("offset", offset);
        if (f.customerId() != null) {
            Long found = jdbc.queryForObject("SELECT count(*) FROM customers WHERE agency_id=:agency AND id=:person",
                    args.addValue("person", f.customerId()), Long.class);
            if (found == null || found == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa não encontrada.");
            args.addValue("personText", f.customerId().toString());
        }
        String union = f.modules().stream().sorted().map(this::source).collect(Collectors.joining(" UNION ALL "));
        String sql = """
                WITH events AS (%s)
                SELECT e.id,e.module,e.resource_id,e.customer_id,e.action,e.created_at,
                       u.id author_id,u.name author_name
                FROM events e LEFT JOIN users u ON u.id::text=e.actor AND u.agency_id=:agency
                WHERE e.agency_id=:agency AND e.product=:product AND e.created_at>=:start AND e.created_at<:end
                %s
                ORDER BY e.created_at DESC,e.module,e.id DESC LIMIT :limit OFFSET :offset
                """.formatted(union, f.customerId() == null ? "" : "AND (e.customer_id=:personText OR e.related_id=:personText)");
        return jdbc.query(sql, args, (rs, n) -> new Event(rs.getObject("id", UUID.class),
                Source.valueOf(rs.getString("module")), rs.getString("resource_id"), rs.getString("customer_id"),
                rs.getString("action"), rs.getObject("author_id", UUID.class),
                rs.getString("author_name") == null ? "Autor não identificado no histórico" : rs.getString("author_name"),
                rs.getObject("created_at", OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC)));
    }

    private static void validate(Filters f) {
        if (f == null || f.product() == null || f.from() == null || f.to() == null ||
                f.modules() == null || f.modules().isEmpty()) invalid("Escolha produto, período e pelo menos um módulo.");
        long days = ChronoUnit.DAYS.between(f.from(), f.to());
        if (days < 0 || days >= 366) invalid("O período deve ser crescente e ter no máximo 366 dias.");
    }
    private static void invalid(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }

    private String source(Source module) {
        // Each branch has an agency predicate; joined origin tables are scoped separately.
        String fields = switch (module) {
            case WALLET -> projection("e.resource_id", "e.customer_id::text", "NULL::text", "'MILES'",
                    "e.action", "e.author", "e.created_at", "customer_miles_audit e");
            case CONTRACTS -> projection("e.resource_id", "e.customer_id::text", "NULL::text",
                    "coalesce(CASE WHEN e.detail->>'product' IN ('TRAVEL','MILES') THEN e.detail->>'product' END,c.product,t.product)", "e.action", "e.author", "e.created_at",
                    "contract_audit e LEFT JOIN contracts c ON c.id=e.resource_id AND c.agency_id=e.agency_id AND e.resource_type='CONTRACT' " +
                            "LEFT JOIN contract_templates t ON t.id=e.resource_id AND t.agency_id=e.agency_id AND e.resource_type='TEMPLATE'");
            case AGENDA -> projection("e.appointment_id", "e.customer_id::text", "NULL::text",
                    "coalesce(CASE WHEN e.detail->>'product' IN ('TRAVEL','MILES') THEN e.detail->>'product' END,a.product)", "e.action", "e.author", "e.created_at",
                    "appointment_events e LEFT JOIN appointments a ON a.id=e.appointment_id AND a.agency_id=e.agency_id");
            case TASKS -> projection("e.resource_id", "e.customer_id::text", "NULL::text",
                    "coalesce(CASE WHEN e.snapshot->>'product' IN ('TRAVEL','MILES') THEN e.snapshot->>'product' END,t.product,r.product,c.product)", "e.action", "e.author_id::text", "e.created_at",
                    "work_task_events e LEFT JOIN work_tasks t ON t.id=e.resource_id AND t.agency_id=e.agency_id AND e.resource_type='TASK' " +
                            "LEFT JOIN checklist_runs r ON r.id=e.resource_id AND r.agency_id=e.agency_id AND e.resource_type='RUN' " +
                            "LEFT JOIN checklist_templates c ON c.id=e.resource_id AND c.agency_id=e.agency_id AND e.resource_type='TEMPLATE'");
            case SIMULATIONS -> projection("e.simulation_id", "e.customer_id::text", "NULL::text", "'MILES'",
                    "'VERSION_RECORDED'", "e.author", "e.recorded_at",
                    "accumulation_simulation_versions e JOIN accumulation_simulations s ON s.id=e.simulation_id AND s.agency_id=:agency",
                    "s.agency_id");
            case PLAN_TASKS -> projection("e.source_id", "e.customer_id::text", "NULL::text", "'MILES'",
                    "'TASKS_GENERATED'", "e.author_id::text", "e.created_at", "simulation_task_generations e");
            case DIAGNOSTICS -> projection("e.diagnostic_id", "d.customer_id::text", "NULL::text", "'MILES'",
                    "e.action", "e.author_id::text", "e.created_at",
                    "miles_diagnostic_events e JOIN miles_diagnostics d ON d.id=e.diagnostic_id AND d.agency_id=e.agency_id AND d.receipt IS NOT NULL");
            case TRIP_STAGES -> projection("e.stage_id", "coalesce(e.detail->>'customerId',t.customer_id::text)", "NULL::text", "'TRAVEL'",
                    "e.action", "e.author", "e.created_at",
                    "trip_stage_events e LEFT JOIN trips t ON t.id=e.trip_id AND t.agency_id=e.agency_id");
            case SAVINGS -> projection("e.resource_id", "e.customer_id::text", "NULL::text", "'TRAVEL'",
                    "e.action", "e.author", "e.created_at", "trip_savings_audit e");
            case REFERRALS -> projection("e.referral_id", "coalesce(e.detail->>'participantReferrerId',r.referrer_customer_id::text)",
                    "coalesce(e.detail->>'participantReferredId',r.referred_customer_id::text)",
                    "coalesce(CASE WHEN e.detail->>'product' IN ('TRAVEL','MILES') THEN e.detail->>'product' END,r.product)", "e.action", "e.author", "e.created_at",
                    "referral_events e LEFT JOIN referrals r ON r.id=e.referral_id AND r.agency_id=e.agency_id");
            case FOLLOWUPS -> projection("e.followup_id", "e.customer_id::text", "NULL::text",
                    "coalesce(CASE WHEN e.snapshot->>'product' IN ('TRAVEL','MILES') THEN e.snapshot->>'product' END,f.product)", "e.action", "e.author_id::text", "e.created_at",
                    "commercial_followup_events e LEFT JOIN commercial_followups f ON f.id=e.followup_id AND f.agency_id=e.agency_id");
        };
        return fields.replace(":module", "'" + module.name() + "'");
    }
    private String projection(String resource, String customer, String related, String product,
                              String action, String actor, String date, String from) {
        return projection(resource, customer, related, product, action, actor, date, from, "e.agency_id");
    }
    private String projection(String resource, String customer, String related, String product,
                              String action, String actor, String date, String from, String agency) {
        return "SELECT e.id," + agency + " agency_id,:module::text module," + resource + "::text resource_id," +
                customer + " customer_id," + related + " related_id," + product + "::text product," +
                action + "::text action," + actor + "::text actor," + date + " created_at FROM " + from +
                " WHERE " + agency + "=:agency";
    }
}
