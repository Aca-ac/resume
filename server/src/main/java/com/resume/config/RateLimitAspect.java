package com.resume.config;

import com.resume.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
public class RateLimitAspect {
    private final StringRedisTemplate redisTemplate;
    public RateLimitAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    @Around("@annotation(rateLimiter)")
    public Object around(ProceedingJoinPoint pjp, RateLimiter rateLimiter) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Long userId = (Long) request.getAttribute("userId");
        String key = rateLimiter.key();
        if (key.isEmpty()) {
            key = "rate_limit:" + (userId != null ? userId : "anonymous");
        } else {
            key = "rate_limit:" + (userId != null ? userId : "anonymous") + ":" + key;
        }
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, rateLimiter.duration(), TimeUnit.SECONDS);
        }
        log.debug("RateLimit key={}, count={}, max={}", key, count, rateLimiter.maxCount());
        if (count != null && count > rateLimiter.maxCount()) {
            throw new BusinessException(429, "请求过于频繁，请稍后再试");
        }
        return pjp.proceed();
    }
}