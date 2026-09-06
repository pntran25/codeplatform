package com.example.codeplatform;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and verifies the JWTs used for authentication.
 *
 * <p>The signing key comes from configuration ({@code jwt.secret}, normally supplied by the
 * {@code JWT_SECRET} environment variable) so that no credential lives in source control. HS256
 * requires at least 256 bits of key material, so the secret must be at least 32 characters.
 */
@Component
public class JwtUtil {

    /** Placeholder shipped in application.properties; safe only for local development. */
    static final String INSECURE_DEV_SECRET = "dev-only-insecure-secret-change-me-in-production";

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey key;
    private final Duration validity;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-minutes:1440}") long expirationMinutes) {
        if (INSECURE_DEV_SECRET.equals(secret)) {
            log.warn("jwt.secret is still the built-in development placeholder. "
                    + "Set the JWT_SECRET environment variable before deploying.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validity = Duration.ofMinutes(expirationMinutes);
    }

    public String generateToken(String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies the signature and expiry of {@code token}.
     *
     * @return the token claims, or {@code null} if the token is missing, malformed, expired or
     *         signed with the wrong key.
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected JWT: {}", e.getMessage());
            return null;
        }
    }
}
