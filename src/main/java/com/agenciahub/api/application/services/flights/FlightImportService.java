package com.agenciahub.api.application.services.flights;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service
public class FlightImportService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper mapper;
    private final FlightDocumentReader documents;
    private final GeminiFlightReader vision;
    private final FlightImportProperties config;
    public FlightImportService(JdbcTemplate jdbc, PlatformTransactionManager manager, ObjectMapper mapper,
                               FlightDocumentReader documents, GeminiFlightReader vision, FlightImportProperties config) {
        this.jdbc = jdbc; this.tx = new TransactionTemplate(manager); this.mapper = mapper;
        this.documents = documents; this.vision = vision; this.config = config;
    }
    public record ImportResult(UUID id, FlightExtraction extraction, boolean cached) {}
    private record Reservation(UUID id, UUID attempt, String cached) {}

    public ImportResult extract(UUID agency, UUID user, String filename, byte[] bytes) throws Exception {
        if (bytes.length == 0 || bytes.length > 4 * 1024 * 1024) throw new IllegalArgumentException("Envie até 4 MB.");
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        String version = "v1:" + config.getModel();
        LocalDate period = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        BigDecimal reserve = config.getReservationUsd().max(new BigDecimal("0.10"));
        String safeName = filename == null ? "oferta" : filename.replaceAll("[\\r\\n]", "");
        safeName = safeName.substring(0, Math.min(255, safeName.length()));
        final String name = safeName;
        Reservation reservation = tx.execute(status -> {
            jdbc.update("INSERT INTO flight_import_usage(agency_id,period) VALUES (?,?) ON CONFLICT DO NOTHING", agency, period);
            var usage = jdbc.queryForMap("SELECT attempts,charged_usd FROM flight_import_usage WHERE agency_id=? AND period=? FOR UPDATE", agency, period);
            // The agency/month lock serializes cache claims and quota reservations across server instances.
            var rows = jdbc.queryForList("SELECT id,status,result::text,created_at FROM flight_imports WHERE agency_id=? AND file_hash=? AND extractor_version=? FOR UPDATE", agency, hash, version);
            UUID id = rows.isEmpty() ? UUID.randomUUID() : (UUID) rows.get(0).get("id");
            UUID attempt = UUID.randomUUID();
            if (!rows.isEmpty() && "DONE".equals(rows.get(0).get("status"))) return new Reservation(id, null, (String) rows.get(0).get("result"));
            if (!rows.isEmpty() && "PROCESSING".equals(rows.get(0).get("status"))
                    && ((java.sql.Timestamp) rows.get(0).get("created_at")).toInstant().isAfter(Instant.now().minusSeconds(300)))
                throw new IllegalStateException("Este arquivo já está em processamento. Aguarde antes de tentar novamente.");
            BigDecimal charged = (BigDecimal) usage.get("charged_usd");
            if (((Number) usage.get("attempts")).intValue() >= config.getMonthlyRequests()
                    || charged.add(reserve).compareTo(config.getMonthlyBudgetUsd()) > 0)
                throw new IllegalStateException("Limite mensal de importações atingido. Continue pelo preenchimento manual.");
            jdbc.update("UPDATE flight_import_usage SET attempts=attempts+1,charged_usd=charged_usd+? WHERE agency_id=? AND period=?", reserve, agency, period);
            if (rows.isEmpty()) jdbc.update("INSERT INTO flight_imports(id,agency_id,file_hash,extractor_version,filename,status,created_by,attempt_id) VALUES (?,?,?,?,?,'PROCESSING',?,?)", id, agency, hash, version, name, user, attempt);
            else jdbc.update("UPDATE flight_imports SET status='PROCESSING',created_at=now(),created_by=?,attempt_id=? WHERE id=? AND agency_id=?", user, attempt, id, agency);
            return new Reservation(id, attempt, null);
        });
        if (reservation.cached() != null) return new ImportResult(reservation.id(), mapper.readValue(reservation.cached(), FlightExtraction.class), true);
        boolean sentToProvider = false;
        try {
            var document = documents.read(bytes);
            FlightExtraction result = document.direct();
            int input = 0, output = 0;
            BigDecimal cost = BigDecimal.ZERO;
            if (result == null) {
                if (!vision.configured()) throw new IllegalStateException("Leitura com IA não configurada. Continue manualmente ou contate o administrador.");
                sentToProvider = true;
                var reading = vision.read(document);
                result = reading.extraction(); input = reading.inputTokens(); output = reading.outputTokens(); cost = reading.cost();
            }
            String json = mapper.writeValueAsString(result);
            final int in = input, out = output;
            final BigDecimal actual = cost;
            tx.executeWithoutResult(status -> {
                lockUsage(agency, period);
                int updated = jdbc.update("UPDATE flight_imports SET status='DONE',result=CAST(? AS jsonb),input_tokens=?,output_tokens=?,estimated_usd=? WHERE id=? AND agency_id=? AND attempt_id=?", json, in, out, actual, reservation.id(), agency, reservation.attempt());
                if (updated != 1) throw new IllegalStateException("Esta tentativa expirou. Aguarde a leitura mais recente.");
                jdbc.update("UPDATE flight_import_usage SET charged_usd=charged_usd-?+? WHERE agency_id=? AND period=?", reserve, actual, agency, period);
            });
            return new ImportResult(reservation.id(), result, false);
        } catch (Exception e) {
            BigDecimal charged = sentToProvider ? reserve : BigDecimal.ZERO;
            tx.executeWithoutResult(status -> {
                lockUsage(agency, period);
                jdbc.update("UPDATE flight_imports SET status='FAILED',estimated_usd=? WHERE id=? AND agency_id=? AND attempt_id=?", charged, reservation.id(), agency, reservation.attempt());
                if (charged.signum() == 0) jdbc.update("UPDATE flight_import_usage SET charged_usd=charged_usd-? WHERE agency_id=? AND period=?", reserve, agency, period);
            });
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            if (e instanceof IllegalStateException || e instanceof IllegalArgumentException) throw e;
            throw new IllegalStateException("Não foi possível ler o arquivo. Envie outro arquivo ou preencha manualmente.");
        }
    }

    /** Same lock order as reservation: usage first, then import. */
    private void lockUsage(UUID agency, LocalDate period) {
        jdbc.queryForObject("SELECT attempts FROM flight_import_usage WHERE agency_id=? AND period=? FOR UPDATE", Integer.class, agency, period);
    }
}
