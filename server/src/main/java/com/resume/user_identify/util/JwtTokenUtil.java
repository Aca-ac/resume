package com.resume.user_identify.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenUtil {

    private static String jwtSecret;
    private static long accessExpirationMinutes;

    @Value("${app.jwt.secret}")
    public void setJwtSecret(String jwtSecret) {
        JwtTokenUtil.jwtSecret = jwtSecret;
    }

    @Value("${app.jwt.access-expiration-minutes}")
    public void setAccessExpirationMinutes(long accessExpirationMinutes) {
        JwtTokenUtil.accessExpirationMinutes = accessExpirationMinutes;
    }

    private static SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateToken(Long userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpirationMinutes * 60 * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public static Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static long getExpirationMinutes() {
        return accessExpirationMinutes;
    }
}