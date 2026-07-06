package com.resume.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.ErrorCode;
import com.resume.common.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String clientIp = resolveClientIp(request);
        Integer limit = null;

        if (path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v1/auth/register")) {
            limit = appProperties.getRateLimit().getLoginMaxPerMinute();
        } else if (path.contains("/optimize") || path.contains("/match/jd") || path.startsWith("/api/v1/interview")) {
            limit = appProperties.getRateLimit().getAiMaxPerMinute();
        }

        if (limit != null && isLimited(path, clientIp, limit)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Result.fail(ErrorCode.TOO_MANY_REQUESTS, "too many requests"));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLimited(String path, String clientIp, int maxPerMinute) {
        String bucket = "rate:" + path + ":" + clientIp;
        Long count = redisTemplate.opsForValue().increment(bucket);
        if (count != null && count == 1L) {
            redisTemplate.expire(bucket, Duration.ofMinutes(1));
        }
        return count != null && count > maxPerMinute;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
