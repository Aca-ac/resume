package com.resume.module.resume.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemplateSaveRequest {
    @NotBlank(message = "模板名称不能为空")
    private String name;
    @NotBlank(message = "分类不能为空")
    private String category;
    private String previewUrl;
    @NotBlank(message = "模板路径不能为空")
    private String templatePath;
    private String applicableScene;
}
