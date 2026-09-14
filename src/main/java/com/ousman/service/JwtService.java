package com.ousman.service;

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
 * JWT Service — written for JJWT 0.12.x API.

 */
@Service
public class JwtService {

    /** Injected from application.properties → jwt.secret */
    @Value("${jwt.secret:ousman-super-secret-jwt-key-minimum-64-characters-long-random-string-here}")
    private String secret;

    /** Injected from application.properties → jwt.expiration (milliseconds, default 24h) */
    @Value("${jwt.expiration:86400000}")
    private long expiration;

    // ── Key builder ───────────────────────────────────────────────────────────

    /**
     * Derives an HMAC-SHA SecretKey from the configured secret string.
     * JJWT 0.12 infers HS256 / HS384 / HS512 automatically from key length:
     *   ≥ 32 bytes → HS256, ≥ 48 bytes → HS384, ≥ 64 bytes → HS512
     */
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ── Token generation ──────────────────────────────────────────────────────

    /**
     * Generates a signed JWT for the given user.
     *
     * @param email user's email address (becomes the token subject)
     * @param role  user's role string (e.g. "ADMIN", "USER")
     * @param name  user's display name
     * @return compact signed JWT string
     */
    public String generateToken(String email, String role, String name) {
        return Jwts.builder()
            .subject(email)                                          // 0.12: subject() not setSubject()
            .claim("role", role)
            .claim("name", name)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getKey())                                      // 0.12: algorithm inferred from key type
            .compact();
    }

    // ── Token parsing ─────────────────────────────────────────────────────────

    /**
     * Extracts the subject (email) from a valid, non-expired JWT.
     *
     * @param token compact JWT string
     * @return the subject claim (user email)
     */
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extracts the role claim from a valid, non-expired JWT.
     *
     * @param token compact JWT string
     * @return the role string stored in the token
     */
    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * Returns true if the token is well-formed, properly signed, and not expired.
     *
     * @param token compact JWT string
     * @return true if valid
     */
    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Covers: ExpiredJwtException, MalformedJwtException, SignatureException, etc.
            return false;
        }
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    /**
     * Parses and verifies the JWT, returning its claims payload.
     * Uses JJWT 0.12 API: Jwts.parser() → verifyWith() → parseSignedClaims() → getPayload()
     *
     * @param token compact JWT string
     * @return verified Claims payload
     * @throws JwtException if token is invalid, expired, or tampered
     */
    private Claims getClaims(String token) {
        return Jwts.parser()                                         // 0.12: parser() not parserBuilder()
            .verifyWith(getKey())                                    // 0.12: verifyWith() not setSigningKey()
            .build()
            .parseSignedClaims(token)                               // 0.12: parseSignedClaims() not parseClaimsJws()
            .getPayload();                                           // 0.12: getPayload() not getBody()
    }
}