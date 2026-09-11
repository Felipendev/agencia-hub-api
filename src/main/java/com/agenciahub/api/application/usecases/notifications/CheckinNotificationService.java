package com.agenciahub.api.application.usecases.notifications;

import com.agenciahub.api.security.SecurityContextUsers;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckinNotificationService {
    private final JdbcTemplate jdbc;

    public CheckinPreferences preferences() {
        return jdbc.query("SELECT * FROM agency_checkin_prefs WHERE agency_id = ?", (rs, n) ->
                new CheckinPreferences(rs.getBoolean("start_enabled"), rs.getInt("start_days"),
                        rs.getBoolean("end_enabled"), rs.getInt("end_days")), TenantContext.requireAgencyId())
                .stream().findFirst().orElseGet(CheckinPreferences::defaults);
    }

    @Transactional
    public CheckinPreferences save(CheckinPreferences prefs) {
        UUID agency = TenantContext.requireAgencyId();
        // Serialize preference edits with their before/after audit entry.
        jdbc.queryForObject("SELECT id FROM agencies WHERE id = ? FOR UPDATE", UUID.class, agency);
        var previous = preferences();
        jdbc.update("""
                INSERT INTO agency_checkin_prefs (agency_id, start_enabled, start_days, end_enabled, end_days)
                VALUES (?, ?, ?, ?, ?) ON CONFLICT (agency_id) DO UPDATE SET
                start_enabled = EXCLUDED.start_enabled, start_days = EXCLUDED.start_days,
                end_enabled = EXCLUDED.end_enabled, end_days = EXCLUDED.end_days, updated_at = NOW()
                """, agency, prefs.startEnabled(), prefs.startDays(), prefs.endEnabled(), prefs.endDays());
        if (!previous.equals(prefs)) {
            jdbc.update("""
                    INSERT INTO agency_audit_log (id, agency_id, user_id, action, field_name, old_value, new_value, created_at)
                    VALUES (?, ?, ?, 'NOTIFICATION_PREFS_UPDATED', 'checkin', ?, ?, NOW())
                    """, UUID.randomUUID(), agency, SecurityContextUsers.requireUserId(), previous.toString(), prefs.toString());
        }
        return prefs;
    }

    public record Notification(UUID id, String type, String title, String message, String link, Instant createdAt, boolean read) {}

    public List<Notification> inbox() {
        return jdbc.query("""
                SELECT * FROM agency_checkin_notifications WHERE agency_id = ? AND dismissed_at IS NULL
                ORDER BY created_at DESC, id LIMIT 200
                """, (rs, n) -> new Notification(rs.getObject("id", UUID.class), "checkin", rs.getString("title"),
                rs.getString("message"), rs.getString("link"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("read_at") != null), TenantContext.requireAgencyId());
    }

    public void read(UUID id) {
        jdbc.update("UPDATE agency_checkin_notifications SET read_at = COALESCE(read_at, NOW()) WHERE agency_id = ? AND id = ?",
                TenantContext.requireAgencyId(), id);
    }

    public void readAll() {
        jdbc.update("UPDATE agency_checkin_notifications SET read_at = NOW() WHERE agency_id = ? AND read_at IS NULL",
                TenantContext.requireAgencyId());
    }

    public void dismissAll() {
        // Retain deduplication keys so the scheduler cannot recreate dismissed events.
        jdbc.update("UPDATE agency_checkin_notifications SET dismissed_at = NOW() WHERE agency_id = ? AND dismissed_at IS NULL",
                TenantContext.requireAgencyId());
    }
}
