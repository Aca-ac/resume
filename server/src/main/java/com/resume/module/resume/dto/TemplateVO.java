package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class TemplateVO {
    private Long id;
    private String name;
    private String category;
    private String previewUrl;
    private String templatePath;
    private String applicableScene;
    private String createdAt;
    private String updatedAt;
}
