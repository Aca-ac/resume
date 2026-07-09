package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class ImportResultVO {
    private Long resumeId;
    private String title;
    private String content;
    private Long fileId;
    private String fileType;
}
