package com.agenciahub.api.security;

import com.agenciahub.api.config.RateLimitProperties;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting por IP em rotas públicas/auth e por token JWT em rotas autenticadas.
 * Executa antes do JWT; não requer autenticação prévia.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATION_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

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
    public void initRules() {
        buckets.clear();
        List<Rule> rules = new ArrayList<>();
        rules.add(new Rule(
                "POST",
                "/public/solicitacao/submit",
                perMinute(properties.getPublicSubmitPerMinute()),
                "public-submit"));
        rules.add(new Rule(
                "POST",
                "/public/solicitacao/consent/revoke",
                perHour(properties.getPublicConsentRevokePerHour()),
                "public-consent-revoke"));
        rules.add(new Rule(
                "POST",
                "/public/data-deletion-request",
                perHour(properties.getPublicDataDeletionPerHour()),
                "public-data-deletion"));
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
                "GET",
                "/public/coupons/validate",
                perMinute(20),
                "public-coupon-validate"));
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
        for (Rule candidate : compiledRules) {
            if (!candidate.method().equalsIgnoreCase(method)) {
                continue;
            }
            if (matcher.match(candidate.pattern(), path)) {
                matched = candidate;
                break;
            }
        }

        if (matched == null) {
            // No IP-based rule matched — check token-based limit for authenticated endpoints
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (!isPlatformAdmin(token)) {
                    boolean isMutation = MUTATION_METHODS.contains(method.toUpperCase());
                    Bandwidth bandwidth = isMutation
                            ? perMinute(properties.getAuthenticatedMutationPerMinute())
                            : perMinute(properties.getAuthenticatedGetPerMinute());
                    String prefix = isMutation ? "auth-token-mut" : "auth-token-get";
                    String tokenKey = prefix + ":" + tokenHash(token);
                    Bucket tokenBucket = buckets.computeIfAbsent(
                            tokenKey,
                            k -> Bucket.builder().addLimit(bandwidth).build());
                    ConsumptionProbe tokenProbe = tokenBucket.tryConsumeAndReturnRemaining(1);
                    if (!tokenProbe.isConsumed()) {
                        rejectTooManyRequests(response, tokenProbe);
                        return;
                    }
                }
            }
            filterChain.doFilter(request, response);
            return;
        }

        final Rule matchedRule = matched;
        String bucketKey = matchedRule.keyPrefix() + ":" + ip;
        Bucket bucket = buckets.computeIfAbsent(
                bucketKey,
                k -> Bucket.builder().addLimit(matchedRule.bandwidth()).build());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }
        rejectTooManyRequests(response, probe);
    }

    private void rejectTooManyRequests(HttpServletResponse response, ConsumptionProbe probe) throws IOException {
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

    private boolean isPlatformAdmin(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return false;
            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode payload = objectMapper.readTree(payloadJson);
            JsonNode roles = payload.get("roles");
            if (roles == null || !roles.isArray()) return false;
            for (JsonNode r : roles) {
                if ("ROLE_PLATFORM_ADMIN".equals(r.asText())) return true;
            }
        } catch (Exception ignored) {
            // malformed token — JWT filter will reject it
        }
        return false;
    }

    private static String tokenHash(String token) {
        int h = token.hashCode();
        return Integer.toHexString(h);
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
