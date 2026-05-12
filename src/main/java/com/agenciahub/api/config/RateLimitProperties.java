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

    /** POST {@code /auth/verify-email} */
    private int authVerifyEmailPerMinute = 40;

    /** POST {@code /auth/reset-password} */
    private int authResetPasswordPerMinute = 20;

    /** POST {@code /auth/register-invite} */
    private int authRegisterInvitePerHour = 15;

    /** GET convite por token (padrão Ant {@code /auth/invite/**}). */
    private int authInviteGetPerMinute = 60;

    /** GET documentação Swagger / OpenAPI (evita abuso leve de leitura). */
    private int docsGetPerMinute = 120;
}
