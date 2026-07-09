package com.resume.module.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.module.resume.entity.Resume;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
}
