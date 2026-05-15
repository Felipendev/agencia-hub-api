package com.agenciahub.api.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

/**
 * Testes HTTP de integração: contexto Spring completo, PostgreSQL embarcado e Flyway.
 * Cada endpoint deve ter pelo menos um cenário feliz e um triste (ver matriz em {@code docs/testing/http-integration-test-matrix.md}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
public abstract class AbstractIntegrationTest {

    private static final EmbeddedPostgres EMBEDDED_POSTGRES;

    static {
        try {
            EMBEDDED_POSTGRES = EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> EMBEDDED_POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected IntegrationHttpSupport http;
}
