package com.agenciahub.api.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Refuses an unsafe production startup instead of silently accepting dev defaults. */
@Component
@Profile({"prod", "production"})
public class ProductionSecurityConfiguration implements InitializingBean {

    static final String DEVELOPMENT_JWT_SECRET = "agenciahub-dev-secret-change-in-production-min32chars";

    private final String jwtSecret;
    private final String allowedOrigins;

    public ProductionSecurityConfiguration(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${cors.allowed-origins}") String allowedOrigins) {
        this.jwtSecret = jwtSecret;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void afterPropertiesSet() {
        validate(jwtSecret, allowedOrigins);
    }

    static void validate(String jwtSecret, String allowedOrigins) {
        if (jwtSecret == null || jwtSecret.isBlank() || DEVELOPMENT_JWT_SECRET.equals(jwtSecret.trim())) {
            throw new IllegalStateException("JWT_SECRET deve ser definido com valor seguro em produção.");
        }
        if (allowedOrigins == null || allowedOrigins.isBlank()
                || "*".equals(allowedOrigins.trim())
                || allowedOrigins.split(",").length == 0) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS deve listar origens explícitas em produção.");
        }
    }
}
