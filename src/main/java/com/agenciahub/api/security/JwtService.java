package com.agenciahub.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * @deprecated Use {@link #generate(UUID, String, UUID, Instant)} instead.
     */
    @Deprecated
    public String generate(UUID userId, String accountKind) {
        return generate(userId, accountKind, null, null);
    }

    /**
     * Generates a JWT token with agency_id and password_changed_at claims.
     *
     * @param userId            the user's UUID
     * @param accountKind       AGENCY_OWNER or SALES_AGENT
     * @param agencyId          the user's agency UUID
     * @param passwordChangedAt the timestamp of the last password change (nullable)
     * @return signed JWT token string
     */
    public String generate(UUID userId, String accountKind, UUID agencyId, Instant passwordChangedAt) {
        long now = System.currentTimeMillis();
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("accountKind", accountKind)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs));

        if (agencyId != null) {
            builder.claim("agency_id", agencyId.toString());
        }

        if (passwordChangedAt != null) {
            builder.claim("password_changed_at", passwordChangedAt.getEpochSecond());
        }

        return builder.signWith(key).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).getSubject());
    }

    /**
     * Lê {@code accountKind} do token; aceita claim legado {@code role} até expirarem tokens antigos.
     */
    public String extractAccountKind(String token) {
        Claims claims = parse(token);
        String accountKind = claims.get("accountKind", String.class);
        if (accountKind != null) {
            return accountKind;
        }
        return claims.get("role", String.class);
    }

    /**
     * @deprecated Use {@link #extractAccountKind(String)}.
     */
    @Deprecated
    public String extractRole(String token) {
        return extractAccountKind(token);
    }

    public UUID extractAgencyId(String token) {
        String agencyId = parse(token).get("agency_id", String.class);
        return agencyId != null ? UUID.fromString(agencyId) : null;
    }

    public Long extractPasswordChangedAt(String token) {
        return parse(token).get("password_changed_at", Long.class);
    }

    public Instant extractIssuedAt(String token) {
        return parse(token).getIssuedAt().toInstant();
    }
}
