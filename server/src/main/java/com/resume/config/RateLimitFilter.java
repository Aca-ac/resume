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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            """
                    local count = redis.call('INCR', KEYS[1])
                    if count == 1 then
                      redis.call('EXPIRE', KEYS[1], ARGV[1])
                    end
                    return count
                    """,
            Long.class);

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
        } else if (!path.contains("/stream")
                && (path.contains("/optimize") || path.contains("/match/jd") || path.startsWith("/api/v1/interview"))) {
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
        Long count = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(bucket),
                String.valueOf(60));
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