package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class ExportResultVO {
    private String exportId;
    private Long resumeId;
    private Long templateId;
    private String format;
    private String filename;
    /** 相对下载路径，如 /api/v1/resumes/exports/{exportId}/download */
    private String downloadUrl;
    private String expiresAt;
}
