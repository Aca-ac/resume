package com.resume.module.auth;

import com.resume.common.BusinessException;
import com.resume.common.ErrorCode;
import com.resume.common.JwtUtils;
import com.resume.module.auth.dto.AuthResponse;
import com.resume.module.auth.dto.LoginRequest;
import com.resume.module.auth.dto.RegisterRequest;
import com.resume.module.user.entity.User;
import com.resume.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_PREFIX = "refresh:";

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    @Transactional(rollbackFor = Exception.class)
    public AuthResponse register(RegisterRequest request) {
        User user = userService.register(request.getUsername(), request.getEmail(), request.getPassword());
        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "invalid credentials"));
        if (!userService.matchesPassword(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "invalid credentials");
        }
        return issueTokens(user);
    }

    public AuthResponse refresh(String refreshToken) {
        String key = REFRESH_PREFIX + refreshToken;
        String userIdStr = redisTemplate.opsForValue().getAndDelete(key);
        if (userIdStr == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "refresh token expired");
        }
        Long userId = Long.parseLong(userIdStr);
        User user = userService.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String access = jwtUtils.createAccessToken(user.getId(), user.getUsername());
        String refresh = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(REFRESH_PREFIX + refresh, String.valueOf(user.getId()), Duration.ofDays(7));
        return new AuthResponse(access, refresh, user.getId(), user.getUsername());
    }
}
