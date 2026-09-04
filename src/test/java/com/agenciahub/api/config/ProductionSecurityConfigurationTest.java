package com.agenciahub.api.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionSecurityConfigurationTest {
    @Test
    void rejectsDevelopmentJwtSecret() {
        assertThrows(IllegalStateException.class, () -> ProductionSecurityConfiguration.validate(
                ProductionSecurityConfiguration.DEVELOPMENT_JWT_SECRET,
                "https://www.agenciashub.com.br"));
    }

    @Test
    void rejectsWildcardCors() {
        assertThrows(IllegalStateException.class, () -> ProductionSecurityConfiguration.validate(
                "a-secure-secret-with-more-than-thirty-two-characters",
                "*"));
    }

    @Test
    void acceptsExplicitConfiguration() {
        assertDoesNotThrow(() -> ProductionSecurityConfiguration.validate(
                "a-secure-secret-with-more-than-thirty-two-characters",
                "https://www.agenciashub.com.br"));
    }
}
