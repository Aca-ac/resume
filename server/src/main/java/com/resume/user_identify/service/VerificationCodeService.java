package com.resume.user_identify.service;

import com.resume.user_identify.dto.ApiResponse;
import com.resume.user_identify.dto.SendCodeResponse;
import com.resume.user_identify.util.EmailSender;
import com.resume.user_identify.util.VerificationCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class VerificationCodeService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private EmailSender emailSender;

    private static final String CODE_PREFIX = "verification_code:";
    private static final String LIMIT_PREFIX = "verification_limit:";
    private static final long CODE_EXPIRE_SECONDS = 300;
    private static final long LIMIT_SECONDS = 60;

    public ApiResponse<SendCodeResponse> sendVerificationCode(String email) {
        String limitKey = LIMIT_PREFIX + email;

        if (redisTemplate.hasKey(limitKey)) {
            SendCodeResponse data = new SendCodeResponse((int) LIMIT_SECONDS);
            return ApiResponse.error(429, "请求过于频繁，请稍后再试");
        }

        String code = VerificationCodeGenerator.generateVerificationCode();
        String codeKey = CODE_PREFIX + email;

        redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(limitKey, "1", LIMIT_SECONDS, TimeUnit.SECONDS);

        emailSender.sendVerificationCode(email, code);

        SendCodeResponse data = new SendCodeResponse((int) CODE_EXPIRE_SECONDS);
        return ApiResponse.success("验证码已发送", data);
    }

    public boolean verifyCode(String email, String inputCode) {
        String key = CODE_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            return false;
        }

        boolean isValid = storedCode.equals(inputCode);

        if (isValid) {
            redisTemplate.delete(key);
        }

        return isValid;
    }
}