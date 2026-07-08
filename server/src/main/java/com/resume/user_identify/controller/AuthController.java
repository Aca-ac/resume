package com.resume.user_identify.controller;

import com.resume.user_identify.dto.*;
import com.resume.user_identify.entity.User;
import com.resume.user_identify.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (userService.isEmailExists(request.getEmail())) {
            return ApiResponse.error(400, "该邮箱已被注册");
        }

        if (!userService.verifyCode(request.getEmail(), request.getCode())) {
            return ApiResponse.error(400, "验证码错误或已过期");
        }

        User user = userService.register(
                request.getEmail(),
                request.getPassword()

        );

        String accessToken = userService.generateToken(user.getId(), user.getEmail());
        long expiresIn = userService.getTokenExpireMinutes() * 60;

        RegisterResponse response = new RegisterResponse(accessToken, expiresIn, user.getId());
        return ApiResponse.success("注册成功", response);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request.getEmail(), request.getPassword());

        if (user == null) {
            return ApiResponse.error(401, "邮箱或密码错误");
        }

        String accessToken = userService.generateToken(user.getId(), user.getEmail());
        long expiresIn = userService.getTokenExpireMinutes() * 60;

        LoginResponse response = new LoginResponse(accessToken, expiresIn, user.getId());
        return ApiResponse.success("登录成功", response);
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshResponse> refresh(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null) {
            return ApiResponse.error(401, "缺少Authorization头");
        }

        String oldToken = null;
        if (authorization.startsWith("Bearer ")) {
            oldToken = authorization.substring(7);
        } else if (authorization.startsWith("Bearer")) {
            oldToken = authorization.substring(6).trim();
        } else {
            oldToken = authorization;
        }

        if (oldToken == null || oldToken.isEmpty()) {
            return ApiResponse.error(401, "无效的Authorization头");
        }

        String newAccessToken = userService.refreshToken(oldToken);

        if (newAccessToken == null) {
            return ApiResponse.error(401, "Token已过期，请重新登录");
        }

        long expiresIn = userService.getTokenExpireMinutes() * 60;
        RefreshResponse response = new RefreshResponse(newAccessToken, expiresIn);
        return ApiResponse.success("刷新成功", response);
    }

    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (!userService.isEmailExists(request.getEmail())) {
            return ApiResponse.error(400, "邮箱未注册");
        }

        boolean success = userService.resetPassword(
                request.getEmail(),
                request.getCode(),
                request.getPassword()
        );

        if (!success) {
            return ApiResponse.error(400, "验证码错误或已过期");
        }

        return ApiResponse.success("密码重置成功", null);
    }
}