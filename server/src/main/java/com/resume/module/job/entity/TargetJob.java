package com.resume.module.job.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("target_jobs")
public class TargetJob {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String jobName;
    private String jdContent;
    private Integer source;
    private Long originalJobId;
    private String sourceUrls;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}