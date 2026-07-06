package com.resume.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtils {

    private final SecretKey key;
    private final long accessExpirationMinutes;

    public JwtUtils(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-expiration-minutes}") long accessExpirationMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMinutes = accessExpirationMinutes;
    }

    public String createAccessToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessExpirationMinutes * 60);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(Map.of("username", username, "type", "access"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public Long getUserId(String token) {
        return Long.parseLong(parse(token).getSubject());
    }
}
