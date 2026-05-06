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
    public String generate(UUID userId, String role) {
        return generate(userId, role, null, null);
    }

    /**
     * Generates a JWT token with agency_id and password_changed_at claims.
     *
     * @param userId           the user's UUID
     * @param role             the user's role (OWNER, SELLER)
     * @param agencyId         the user's agency UUID
     * @param passwordChangedAt the timestamp of the last password change (nullable)
     * @return signed JWT token string
     */
    public String generate(UUID userId, String role, UUID agencyId, Instant passwordChangedAt) {
        long now = System.currentTimeMillis();
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
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

    public String extractRole(String token) {
        return parse(token).get("role", String.class);
    }

    public UUID extractAgencyId(String token) {
        String agencyId = parse(token).get("agency_id", String.class);
        return agencyId != null ? UUID.fromString(agencyId) : null;
    }

    public Long extractPasswordChangedAt(String token) {
        return parse(token).get("password_changed_at", Long.class);
    }
}
