package com.resume.user_identify.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class VerificationCodeGenerator {
    
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int CODE_LENGTH = 6;
    
    /**
     * 生成6位随机数字验证码
     * @return 6位数字字符串
     */
    public static String generateVerificationCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(secureRandom.nextInt(10)); // 生成0-9之间的随机数
        }
        return code.toString();
    }
}
