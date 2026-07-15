package com.resume.module.job.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_semantic_vector")
public class JobSemanticVector {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long jobId;
    private String vector;
    private Integer modifiedFlag;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}