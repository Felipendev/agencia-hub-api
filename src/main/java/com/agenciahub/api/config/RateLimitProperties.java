package com.agenciahub.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limites por IP (ou X-Forwarded-For, se {@link #trustXForwardedFor} estiver ativo).
 * Em várias instâncias, cada JVM tem seu próprio contador; use WAF/API gateway para limite global.
 */
@Data
@ConfigurationProperties(prefix = "agenciahub.ratelimit")
public class RateLimitProperties {

    /** Desliga o filtro (útil em testes). */
    private boolean enabled = true;

    /**
     * Quando true, o primeiro IP de {@code X-Forwarded-For} é usado como chave.
     * Ative apenas atrás de proxy confiável (Railway, Nginx, etc.).
     */
    private boolean trustXForwardedFor = false;

    /** POST {@code /public/solicitacao/submit} */
    private int publicSubmitPerMinute = 15;

    /** GET configs públicas (padrão Ant {@code /public/solicitacao-config/**}). */
    private int publicConfigGetPerMinute = 120;

    /** GET {@code /public/terms/latest} */
    private int publicTermsGetPerMinute = 120;

    /** POST {@code /auth/login} */
    private int authLoginPerMinute = 30;

    /** POST {@code /auth/register} */
    private int authRegisterPerHour = 15;

    /** POST {@code /auth/forgot-password} */
    private int authForgotPasswordPerHour = 8;

    /** POST {@code /auth/resend-code} */
    private int authResendCodePerHour = 10;

    /** POST {@code /auth/verify-email} — OTP; máx 5 tentativas por IP por minuto (SEC-02) */
    private int authVerifyEmailPerMinute = 5;

    /** POST {@code /auth/reset-password} — OTP; máx 5 tentativas por IP por minuto (SEC-02) */
    private int authResetPasswordPerMinute = 5;

    /** POST {@code /auth/register-invite} */
    private int authRegisterInvitePerHour = 15;

    /** GET convite por token (padrão Ant {@code /auth/invite/**}). */
    private int authInviteGetPerMinute = 60;

    /** GET documentação Swagger / OpenAPI (evita abuso leve de leitura). */
    private int docsGetPerMinute = 120;

    /** POST {@code /public/solicitacao/consent/revoke} — LGPD revogação; máx 3/hora por IP (SEC-03) */
    private int publicConsentRevokePerHour = 3;

    /** POST {@code /public/data-deletion-request} — LGPD exclusão; máx 3/hora por IP (LGPD-02) */
    private int publicDataDeletionPerHour = 3;

    // ── Rate limit por token JWT (SEC-06) ─────────────────────────────────────

    /** GET autenticados: máx requisições por minuto por token JWT */
    private int authenticatedGetPerMinute = 300;

    /** POST/PUT/PATCH/DELETE autenticados: máx requisições por minuto por token JWT */
    private int authenticatedMutationPerMinute = 60;
}
