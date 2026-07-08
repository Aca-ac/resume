package com.resume.user_identify.controller;

import com.resume.user_identify.dto.ApiResponse;
import com.resume.user_identify.entity.User;
import com.resume.user_identify.service.UserService;
import com.resume.user_identify.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public ApiResponse<User> getProfile(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = JwtTokenUtil.getUserIdFromAuthorization(authorization);
        if (userId == null) {
            return ApiResponse.error(401, "无效的Token或Token已过期");
        }

        User user = userService.getUserInfo(userId);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        user.setPasswordHash(null);
        return ApiResponse.success("获取成功", user);
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody User updateRequest) {
        Long userId = JwtTokenUtil.getUserIdFromAuthorization(authorization);
        if (userId == null) {
            return ApiResponse.error(401, "无效的Token或Token已过期");
        }

        User existingUser = userService.getUserInfo(userId);
        if (existingUser == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        if (updateRequest.getNickname() != null) {
            existingUser.setNickname(updateRequest.getNickname());
        }
        if (updateRequest.getName() != null) {
            existingUser.setName(updateRequest.getName());
        }
        if (updateRequest.getPhone() != null) {
            existingUser.setPhone(updateRequest.getPhone());
        }
        if (updateRequest.getBirthDate() != null) {
            existingUser.setBirthDate(updateRequest.getBirthDate());
        }
        if (updateRequest.getEducation() != null) {
            existingUser.setEducation(updateRequest.getEducation());
        }
        if (updateRequest.getWorkYears() != null) {
            existingUser.setWorkYears(updateRequest.getWorkYears());
        }
        if (updateRequest.getCity() != null) {
            existingUser.setCity(updateRequest.getCity());
        }

        boolean success = userService.updateUserInfo(existingUser);
        if (!success) {
            return ApiResponse.error(500, "更新失败");
        }

        return ApiResponse.success("更新成功", null);
    }
}
