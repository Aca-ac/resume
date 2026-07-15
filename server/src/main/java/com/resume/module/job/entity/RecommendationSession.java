package com.resume.module.job.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recommendation_session")
public class RecommendationSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long resumeId;
    private Integer hasMatchResult;
    private String problemDescription;
    private String improvementSuggestions;
    private String suggestionPriority;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}