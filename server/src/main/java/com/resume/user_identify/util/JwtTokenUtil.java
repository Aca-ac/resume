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
    private static long refreshGraceMinutes = 5;

    @Value("${app.jwt.secret}")
    public void setJwtSecret(String jwtSecret) {
        JwtTokenUtil.jwtSecret = jwtSecret;
    }

    @Value("${app.jwt.access-expiration-minutes}")
    public void setAccessExpirationMinutes(long accessExpirationMinutes) {
        JwtTokenUtil.accessExpirationMinutes = accessExpirationMinutes;
    }

    @Value("${app.jwt.refresh-grace-minutes:5}")
    public void setRefreshGraceMinutes(long refreshGraceMinutes) {
        JwtTokenUtil.refreshGraceMinutes = refreshGraceMinutes;
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

    public static String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("email", String.class);
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

    public static boolean isTokenExpiredBeyondGracePeriod(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Date expiration = claims.getExpiration();
            Date now = new Date();

            if (expiration.after(now)) {
                // token未过期，不算超出宽容期
                return false;
            }

            // token已过期，计算过期时长是否超出宽限期
            long expiredMinutes = (now.getTime() - expiration.getTime()) / (60 * 1000);
            return expiredMinutes > refreshGraceMinutes;
        } catch (Exception e) {
            return true;
        }
    }


    public static Long getUserIdFromAuthorization(String authorization) {
        if (authorization == null) {
            return null;
        }

        String token = null;
        if (authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        } else if (authorization.startsWith("Bearer")) {
            token = authorization.substring(6).trim();
        } else {
            token = authorization;
        }

        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            return getUserIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    public static long getExpirationMinutes() {
        return accessExpirationMinutes;
    }
}