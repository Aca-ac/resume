package com.resume.module.template.dto;

import lombok.Data;

@Data
public class TemplateUploadVO {
    private String type;
    private String path;
    private String url;
    private String originalName;
    private long size;
}
