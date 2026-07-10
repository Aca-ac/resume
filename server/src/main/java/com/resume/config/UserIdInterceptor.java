package com.resume.config;

import com.resume.user_identify.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class UserIdInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        Long userId = JwtTokenUtil.getIdFromOriginalToken(authorization);

        if (userId != null) {
            request.setAttribute("userId", userId);
        } else {
            log.warn("Invalid or missing token in request: {}", request.getRequestURI());
        }

        return true;
    }
}