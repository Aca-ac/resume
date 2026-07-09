package com.resume.module.resume.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("resume_files")
public class ResumeFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long resumeId;
    private String fileType;
    private String filePath;
    private Long fileSize;
    private String originalName;
    private String ocrText;
    private LocalDateTime createdAt;
}
