package com.agenciahub.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * NOTIF-02: stateless JWT tokens for email unsubscribe links (30-day expiry, no DB state).
 */
@Service
public class UnsubscribeTokenService {

    private static final long EXPIRY_MS = 30L * 24 * 60 * 60 * 1000;

    private final SecretKey key;

    public UnsubscribeTokenService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(String email, String notifType) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim("notif_type", notifType)
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRY_MS))
                .signWith(key)
                .compact();
    }

    public record UnsubscribeClaims(String email, String notifType) {}

    /** @throws JwtException if token is invalid or expired */
    public UnsubscribeClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new UnsubscribeClaims(
                claims.getSubject(),
                claims.get("notif_type", String.class));
    }
}
