package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class ExportResultVO {
    private String exportId;
    private Long resumeId;
    private Long templateId;
    private String format;
    private String filename;
    /** 相对 API 前缀的下载路径，如 /v1/resumes/exports/{exportId}/download（axios baseURL=/api 时勿再含 /api） */
    private String downloadUrl;
    private String expiresAt;
}
