package com.resume.user_identify.controller;

import com.resume.user_identify.dto.ApiResponse;
import com.resume.user_identify.dto.RegisterRequest;
import com.resume.user_identify.dto.RegisterResponse;
import com.resume.user_identify.entity.User;
import com.resume.user_identify.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}