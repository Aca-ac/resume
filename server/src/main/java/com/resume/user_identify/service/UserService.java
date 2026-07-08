package com.resume.user_identify.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.user_identify.entity.User;
import com.resume.user_identify.mapper.UserMapper;
import com.resume.user_identify.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public boolean isEmailExists(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return userMapper.selectCount(wrapper) > 0;
    }

    public User register(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));

        user.setStatus(1);

        userMapper.insert(user);
        return user;
    }

    public boolean verifyCode(String email, String code) {
        return verificationCodeService.verifyCode(email, code);
    }

    public String generateToken(Long userId, String email) {
        return jwtTokenUtil.generateToken(userId, email);
    }

    public long getTokenExpireMinutes() {
        return jwtTokenUtil.getExpirationMinutes();
    }
}