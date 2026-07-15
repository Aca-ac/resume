package com.resume.module.job.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("match_recommendation")
public class MatchRecommendation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long userId;
    private Long resumeId;
    private Long jobId;
    private String jobName;
    private String jdContent;
    private Integer matchScore;
    private String matchReason;
    private Integer source;
    private String sourceUrl;
    private Long sourceJobId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}