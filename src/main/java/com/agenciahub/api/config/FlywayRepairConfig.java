package com.agenciahub.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 3.4 não expõe {@code spring.flyway.repair-on-migrate}. Quando o checksum de uma
 * migração já aplicada deixa de bater com o ficheiro no classpath (ex.: V13 alterada após deploy),
 * o {@code migrate()} falha antes de arrancar o Tomcat. {@link org.flywaydb.core.Flyway#repair()}
 * realinha os checksums na tabela de histórico sem reexecutar SQL já aplicado.
 */
@Configuration
public class FlywayRepairConfig {

    @Bean
    @ConditionalOnProperty(
            prefix = "agenciahub.flyway",
            name = "repair-before-migrate",
            havingValue = "true",
            matchIfMissing = true)
    FlywayMigrationStrategy flywayRepairThenMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
