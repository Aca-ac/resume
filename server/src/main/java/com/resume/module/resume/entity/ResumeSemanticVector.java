package com.resume.module.resume.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resume_semantic_vector")
public class ResumeSemanticVector {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resumeId;
    private String vector;
    private Integer modifiedFlag;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}