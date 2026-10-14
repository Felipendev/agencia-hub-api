package com.agenciahub.api.application.services.savings;

import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.exception.SavingsConflictException;
import com.agenciahub.api.security.SecurityContextUsers;
import com.agenciahub.api.security.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Agência, autor, auditoria e JSON do módulo de economia. */
@Component
@RequiredArgsConstructor
class SavingsSupport {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public record AuditEntry(UUID id, String action, Object detail, String author, OffsetDateTime createdAt) {}

    UUID agency() { return TenantContext.requireAgencyId(); }

    /** Nome e e-mail do usuário autenticado; nunca inclui senha ou token. */
    String author() {
        PlatformAccount user = SecurityContextUsers.requireUser();
        return user.getName() + " <" + user.getEmail() + ">";
    }

    boolean isOwner() {
        return SecurityContextUsers.optionalUser().map(u -> u.getAccountKind() == AccountKind.AGENCY_OWNER).orElse(false);
    }

    void expectVersion(int actual, Integer expected) {
        if (expected == null || actual != expected) {
            throw new SavingsConflictException("A análise foi alterada por outra pessoa. Recarregue antes de salvar.");
        }
    }

    void audit(String type, UUID resourceId, UUID tripId, UUID customerId, String action, Object detail) {
        jdbc.update("""
                INSERT INTO trip_savings_audit(id, agency_id, resource_type, resource_id, trip_id, customer_id, action, detail, author)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)""",
                UUID.randomUUID(), agency(), type, resourceId, tripId, customerId, action, json(detail), author());
    }

    List<AuditEntry> history(UUID tripId) {
        return jdbc.query("""
                SELECT id, action, detail, author, created_at FROM trip_savings_audit
                WHERE agency_id = ? AND trip_id = ? ORDER BY created_at DESC, id""",
                (rs, n) -> new AuditEntry(rs.getObject("id", UUID.class), rs.getString("action"), read(rs.getString("detail"), Object.class),
                        rs.getString("author"), rs.getObject("created_at", OffsetDateTime.class)),
                agency(), tripId);
    }

    String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível registrar a análise de economia.", e);
        }
    }

    <T> T read(String value, Class<T> type) {
        try {
            return value == null ? null : mapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível consultar a análise de economia.", e);
        }
    }
}
