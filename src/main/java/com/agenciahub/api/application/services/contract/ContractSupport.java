package com.agenciahub.api.application.services.contract;

import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.domain.contract.Contracts.AuditEntry;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.exception.ContractConflictException;
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

/** Autor, agência, auditoria e controle de versão compartilhados pelos serviços de contratos. */
@Component
@RequiredArgsConstructor
class ContractSupport {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    UUID agency() { return TenantContext.requireAgencyId(); }

    /** Nome e e-mail do usuário autenticado; nunca inclui senha ou token. */
    String author() {
        PlatformAccount user = SecurityContextUsers.requireUser();
        return user.getName() + " <" + user.getEmail() + ">";
    }

    boolean isOwner() {
        return SecurityContextUsers.optionalUser()
                .map(user -> user.getAccountKind() == AccountKind.AGENCY_OWNER)
                .orElse(false);
    }

    void expectVersion(int actual, Integer expected) {
        if (expected == null || actual != expected) {
            throw new ContractConflictException("O registro foi alterado por outra pessoa. Recarregue antes de continuar.");
        }
    }

    void audit(String resourceType, UUID resourceId, UUID customerId, String action, Object detail) {
        jdbc.update("""
                INSERT INTO contract_audit(id, agency_id, resource_type, resource_id, customer_id, action, detail, author)
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?)""",
                UUID.randomUUID(), agency(), resourceType, resourceId, customerId, action, json(detail), author());
    }

    List<AuditEntry> history(UUID resourceId) {
        return jdbc.query("""
                SELECT id, action, detail, author, created_at FROM contract_audit
                WHERE agency_id = ? AND resource_id = ? ORDER BY created_at DESC, id""",
                (rs, n) -> new AuditEntry(rs.getObject("id", UUID.class), rs.getString("action"),
                        read(rs.getString("detail")), rs.getString("author"), rs.getObject("created_at", OffsetDateTime.class)),
                agency(), resourceId);
    }

    String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível registrar o contrato.", e);
        }
    }

    Object read(String value) {
        try {
            return value == null ? null : mapper.readValue(value, Object.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível consultar o contrato.", e);
        }
    }
}
