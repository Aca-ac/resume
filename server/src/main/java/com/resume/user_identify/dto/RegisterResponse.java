package com.resume.user_identify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterResponse {
    private String accessToken;
    private Long expiresInSeconds;
    private Long userId;
}