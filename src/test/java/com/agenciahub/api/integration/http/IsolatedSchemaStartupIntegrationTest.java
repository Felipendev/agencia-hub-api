package com.agenciahub.api.integration.http;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/** Starts the actual application with the preview schema configuration and an existing public schema. */
@SpringBootTest(properties = {
        "spring.flyway.default-schema=f03_v58_review",
        "spring.flyway.schemas=f03_v58_review",
        "spring.flyway.create-schemas=true",
        "spring.jpa.properties.hibernate.default_schema=f03_v58_review",
        "agenciahub.flyway.repair-before-migrate=false"
})
@ActiveProfiles("test")
class IsolatedSchemaStartupIntegrationTest {
    private static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
            try (Connection connection = POSTGRES.getPostgresDatabase().getConnection();
                 var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE public.preview_history_marker (value text)");
                statement.execute("INSERT INTO public.preview_history_marker VALUES ('preserved')");
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("DB_HOST", () -> "localhost");
        registry.add("DB_PORT", POSTGRES::getPort);
        registry.add("DB_NAME", () -> "postgres");
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?currentSchema=f03_v58_review,public");
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    @Autowired JdbcTemplate jdbc;

    @Test
    void startsWithV58InIsolatedSchemaAndPreservesPublicData() {
        assertThat(jdbc.queryForObject("SELECT current_schema()", String.class)).isEqualTo("f03_v58_review");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM f03_v58_review.flyway_schema_history WHERE version = '58' AND success", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT value FROM public.preview_history_marker", String.class)).isEqualTo("preserved");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'flyway_schema_history'", Integer.class)).isZero();
    }
}
