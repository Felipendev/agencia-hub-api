package com.agenciahub.api.security;

import com.agenciahub.api.exception.MissingAgencyContextException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void get_returnsNull_whenNeverSet() {
        assertNull(TenantContext.get());
    }

    @Test
    void setAndGet_roundTrip() {
        UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");
        TenantContext.set(id);
        assertEquals(id, TenantContext.get());
    }

    @Test
    void requireAgencyId_returnsValue_whenSet() {
        UUID id = UUID.fromString("44444444-4444-4444-4444-444444444444");
        TenantContext.set(id);
        assertEquals(id, TenantContext.requireAgencyId());
    }

    @Test
    void requireAgencyId_throws_whenMissing() {
        MissingAgencyContextException ex = assertThrows(MissingAgencyContextException.class,
                TenantContext::requireAgencyId);
        assertEquals("agência não definida no contexto da requisição", ex.getMessage());
    }

    @Test
    void clear_removesValue() {
        TenantContext.set(UUID.randomUUID());
        TenantContext.clear();
        assertNull(TenantContext.get());
    }
}
