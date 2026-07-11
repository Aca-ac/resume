package com.resume.module.resume.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resume_details")
public class ResumeDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resumeId;
    private String sectionType;
    private String sectionName;
    private String content;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
