package com.agenciahub.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Compatibilidade para recuperação manual explícita. Desativado por padrão:
 * inicialização normal valida o histórico e nunca realinha checksums automaticamente.
 * Não habilitar para contornar divergências em migrations publicadas.
 */
@Configuration
public class FlywayRepairConfig {

    @Bean
    @ConditionalOnProperty(
            prefix = "agenciahub.flyway",
            name = "repair-before-migrate",
            havingValue = "true",
            matchIfMissing = false)
    FlywayMigrationStrategy flywayRepairThenMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
