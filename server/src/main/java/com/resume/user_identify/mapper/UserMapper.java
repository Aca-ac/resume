package com.resume.user_identify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.user_identify.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}