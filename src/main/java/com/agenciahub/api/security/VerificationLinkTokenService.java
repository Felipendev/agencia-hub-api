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
 * Signs short-lived JWT tokens embedded in email verification links.
 * Uses a dedicated secret so these tokens are structurally distinct from auth JWTs.
 */
@Service
public class VerificationLinkTokenService {

    private static final String PURPOSE = "email-verification-link";
    private static final long EXPIRY_MS = 15 * 60 * 1_000L; // 15 minutes

    private final SecretKey key;

    public VerificationLinkTokenService(
            @Value("${verification.link.secret:${jwt.secret}}") String secret) {
        // Derive a different key by appending a domain suffix so the same raw secret
        // cannot be used interchangeably with auth tokens.
        String derived = secret + ":verify-link";
        this.key = Keys.hmacShaKeyFor(derived.getBytes(StandardCharsets.UTF_8));
    }

    /** Generates an opaque token that encodes email + code for 15 minutes. */
    public String generate(String email, String code) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim("code", code)
                .claim("purpose", PURPOSE)
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRY_MS))
                .signWith(key)
                .compact();
    }

    public record LinkPayload(String email, String code) {}

    /**
     * @throws IllegalArgumentException if the token is invalid, expired, or not a verification link.
     */
    public LinkPayload parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!PURPOSE.equals(claims.get("purpose", String.class))) {
                throw new IllegalArgumentException("token de verificação inválido");
            }

            String email = claims.getSubject();
            String code  = claims.get("code", String.class);
            if (email == null || code == null) {
                throw new IllegalArgumentException("token de verificação incompleto");
            }
            return new LinkPayload(email, code);
        } catch (JwtException e) {
            throw new IllegalArgumentException("link de verificação inválido ou expirado");
        }
    }
}
