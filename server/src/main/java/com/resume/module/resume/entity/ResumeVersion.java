package com.resume.module.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resume_versions")
public class ResumeVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resumeId;
    private Integer versionNo;
    private String content;
    private LocalDateTime createdAt;
}
