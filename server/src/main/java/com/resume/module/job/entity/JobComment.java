package com.resume.module.job.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_comments")
public class JobComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long jobId;
    private Long userId;
    private String content;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}