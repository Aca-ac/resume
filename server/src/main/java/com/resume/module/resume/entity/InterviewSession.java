package com.resume.module.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_sessions")
public class InterviewSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long resumeId;
    private String jobTitle;
    private String status;
    private String report;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
