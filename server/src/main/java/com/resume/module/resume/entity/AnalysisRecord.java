package com.resume.module.resume.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("analysis_records")
public class AnalysisRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long resumeId;
    private Integer summaryScore;
    private Integer educationScore;
    private Integer experienceScore;
    private Integer skillScore;
    private Integer projectScore;
    private Integer totalScore;
    private String suggestions;
    private Integer status;
    private String errorMessage;
    private String requestId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
