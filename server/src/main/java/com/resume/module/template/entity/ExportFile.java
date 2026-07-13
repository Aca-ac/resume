package com.resume.module.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("export_files")
public class ExportFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long resumeId;
    private Long templateId;
    /** WORD / PDF */
    private String fileType;
    private String filePath;
    private String originalName;
    private LocalDateTime expireAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
