package com.resume.user_identify.controller;

import com.resume.user_identify.dto.ApiResponse;
import com.resume.user_identify.dto.SendCodeRequest;
import com.resume.user_identify.dto.SendCodeResponse;
import com.resume.user_identify.service.VerificationCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class VerificationCodeController {

    @Autowired
    private VerificationCodeService verificationCodeService;

    @PostMapping("/code/send")
    public ResponseEntity<ApiResponse<SendCodeResponse>> sendVerificationCode(
            @Validated @RequestBody SendCodeRequest request) {
        log.info("发送验证码请求: {}", request);

        ApiResponse<SendCodeResponse> result = verificationCodeService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok(result);
    }
}