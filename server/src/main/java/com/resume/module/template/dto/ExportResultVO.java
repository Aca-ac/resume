package com.resume.module.template.dto;

import lombok.Data;

@Data
public class ExportResultVO {
    private Long fileId;
    private String fileType;
    private String originalName;
    private String downloadUrl;
    private String expireAt;
}
