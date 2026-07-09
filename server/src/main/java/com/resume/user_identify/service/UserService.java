package com.resume.user_identify.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.user_identify.entity.User;
import com.resume.user_identify.mapper.UserMapper;
import com.resume.user_identify.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
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

    public User login(String email, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            return null;
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            return null;
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return null;
        }

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        return user;
    }

    public String refreshToken(String oldToken) {
        if (JwtTokenUtil.isTokenExpiredBeyondGracePeriod(oldToken)) {
            log.warn("Token has expired beyond grace period: {}", oldToken);
            return null;
        }

        Long userId = JwtTokenUtil.getUserIdFromToken(oldToken);
        String email = JwtTokenUtil.getEmailFromToken(oldToken);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getId, userId);
        User user = userMapper.selectOne(wrapper);

        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            return null;
        }

        return jwtTokenUtil.generateToken(userId, email);
    }

    public boolean resetPassword(String email, String code, String newPassword) {
        if (!verificationCodeService.verifyCode(email, code)) {
            return false;
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            return false;
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        return true;
    }

    public User getUserInfo(Long userId) {
        return userMapper.selectById(userId);
    }

    public boolean updateUserInfo(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        return userMapper.updateById(user) > 0;
    }

}