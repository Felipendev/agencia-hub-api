package com.agenciahub.api.security;

import com.agenciahub.api.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting por IP em rotas públicas e de autenticação.
 * Executa antes do JWT; não exige autenticação.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    private final UrlPathHelper urlPathHelper = new UrlPathHelper();
    private final AntPathMatcher matcher = new AntPathMatcher();

    private record Rule(String method, String pattern, Bandwidth bandwidth, String keyPrefix) {}

    private volatile List<Rule> compiledRules = List.of();
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @jakarta.annotation.PostConstruct
    void onInit() {
        initRules();
    }

    /** Recalcula limites a partir das propriedades (testes podem chamar após ajustar limites). */
    void initRules() {
        buckets.clear();
        List<Rule> rules = new ArrayList<>();
        rules.add(new Rule(
                "POST",
                "/public/solicitacao/submit",
                perMinute(properties.getPublicSubmitPerMinute()),
                "public-submit"));
        rules.add(new Rule(
                "GET",
                "/public/solicitacao-config/**",
                perMinute(properties.getPublicConfigGetPerMinute()),
                "public-config"));
        rules.add(new Rule(
                "GET",
                "/public/terms/latest",
                perMinute(properties.getPublicTermsGetPerMinute()),
                "public-terms"));
        rules.add(new Rule(
                "POST",
                "/auth/login",
                perMinute(properties.getAuthLoginPerMinute()),
                "auth-login"));
        rules.add(new Rule(
                "POST",
                "/auth/register",
                perHour(properties.getAuthRegisterPerHour()),
                "auth-register"));
        rules.add(new Rule(
                "POST",
                "/auth/forgot-password",
                perHour(properties.getAuthForgotPasswordPerHour()),
                "auth-forgot"));
        rules.add(new Rule(
                "POST",
                "/auth/resend-code",
                perHour(properties.getAuthResendCodePerHour()),
                "auth-resend"));
        rules.add(new Rule(
                "POST",
                "/auth/verify-email",
                perMinute(properties.getAuthVerifyEmailPerMinute()),
                "auth-verify"));
        rules.add(new Rule(
                "POST",
                "/auth/reset-password",
                perMinute(properties.getAuthResetPasswordPerMinute()),
                "auth-reset"));
        rules.add(new Rule(
                "POST",
                "/auth/register-invite",
                perHour(properties.getAuthRegisterInvitePerHour()),
                "auth-register-invite"));
        rules.add(new Rule(
                "GET",
                "/auth/invite/**",
                perMinute(properties.getAuthInviteGetPerMinute()),
                "auth-invite-get"));
        rules.add(new Rule(
                "GET",
                "/v3/api-docs/**",
                perMinute(properties.getDocsGetPerMinute()),
                "docs-openapi"));
        rules.add(new Rule(
                "GET",
                "/swagger-ui/**",
                perMinute(properties.getDocsGetPerMinute()),
                "docs-swagger"));
        rules.add(new Rule(
                "GET",
                "/swagger-ui.html",
                perMinute(properties.getDocsGetPerMinute()),
                "docs-swagger-root"));
        rules.add(new Rule(
                "GET",
                "/docs",
                perMinute(properties.getDocsGetPerMinute()),
                "docs-redirect"));
        this.compiledRules = List.copyOf(rules);
    }

    private static Bandwidth perMinute(int tokens) {
        return Bandwidth.classic(tokens, Refill.intervally(tokens, Duration.ofMinutes(1)));
    }

    private static Bandwidth perHour(int tokens) {
        return Bandwidth.classic(tokens, Refill.intervally(tokens, Duration.ofHours(1)));
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = urlPathHelper.getPathWithinApplication(request);
        String method = request.getMethod();
        String ip = clientIp(request);

        Rule matched = null;
        for (Rule rule : compiledRules) {
            if (!rule.method.equalsIgnoreCase(method)) {
                continue;
            }
            if (matcher.match(rule.pattern, path)) {
                matched = rule;
                break;
            }
        }

        if (matched == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucketKey = matched.keyPrefix + ":" + ip;
        Bucket bucket = buckets.computeIfAbsent(
                bucketKey,
                k -> Bucket.builder().addLimit(matched.bandwidth()).build());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(
                1L,
                java.util.concurrent.TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "message", "Muitas requisições. Aguarde e tente novamente.",
                "code", "TOO_MANY_REQUESTS")));
    }

    private String clientIp(HttpServletRequest request) {
        if (properties.isTrustXForwardedFor()) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        String addr = request.getRemoteAddr();
        return addr != null ? addr : "unknown";
    }
}
