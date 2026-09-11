package com.agenciahub.api.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class CheckinNotificationScheduler {
    private final JdbcTemplate jdbc;

    @Scheduled(fixedDelayString = "${notifications.checkin-delay-ms:60000}")
    @Transactional
    public void run() { generate(LocalDate.now(ZoneId.of("America/Sao_Paulo"))); }

    /** One atomic insert per run, safe across replicas and retries. Catch up only through event day. */
    public int generate(LocalDate today) {
        return jdbc.update("""
                WITH sources AS (
                    SELECT t.agency_id,
                           COALESCE('quotation-' || t.quotation_id::text, 'trip-' || t.id::text) AS source_key,
                           '/viagens/' || t.id AS link,
                           'Viagem ' || COALESCE(t.booking_locator, t.id::text) AS label,
                           t.travel_start_date AS start_date, t.travel_end_date AS end_date
                    FROM trips t WHERE t.status IN ('UPCOMING', 'IN_PROGRESS')
                    UNION ALL
                    SELECT q.agency_id, 'quotation-' || q.id::text, '/cotacoes/' || q.id, q.title,
                           q.travel_start_date, q.travel_end_date
                    FROM quotations q WHERE q.status = 'ACCEPTED'
                      AND NOT EXISTS (SELECT 1 FROM trips t WHERE t.quotation_id = q.id AND t.agency_id = q.agency_id)
                ), events AS (
                    SELECT s.*, e.kind, e.event_date, e.enabled, e.days
                    FROM sources s JOIN agency_checkin_prefs p ON p.agency_id = s.agency_id
                    JOIN agencies a ON a.id = s.agency_id AND a.status = 'ACTIVE'
                    CROSS JOIN LATERAL (VALUES
                        ('start', s.start_date, p.start_enabled, p.start_days),
                        ('end', s.end_date, p.end_enabled, p.end_days)
                    ) AS e(kind, event_date, enabled, days)
                    WHERE e.enabled AND e.event_date BETWEEN CAST(? AS date) AND CAST(? AS date) + e.days
                )
                INSERT INTO agency_checkin_notifications (id, agency_id, event_key, event_date, title, message, link)
                SELECT gen_random_uuid(), agency_id, source_key || '-' || kind || '-' || event_date::text, event_date,
                       CASE kind WHEN 'start' THEN 'Check-in: início da viagem' ELSE 'Check-in: fim da viagem' END,
                       label || ' · ' || CASE kind WHEN 'start' THEN 'Início' ELSE 'Fim' END || ' em ' || to_char(event_date, 'DD/MM/YYYY'), link
                FROM events ON CONFLICT (agency_id, event_key) DO NOTHING
                """, today, today);
    }
}
