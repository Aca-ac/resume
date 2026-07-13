package com.resume.module.template.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemplateCreateRequest {
    @NotBlank
    private String name;
    private String category;
    private String applicableScene;
    /** 已上传的模板相对路径（可与 multipart 二选一） */
    private String templatePath;
    private String previewUrl;
}
