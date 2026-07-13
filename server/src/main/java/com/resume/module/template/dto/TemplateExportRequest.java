package com.resume.module.template.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TemplateExportRequest {
    @NotNull
    private Long templateId;
    @NotNull
    private Long resumeId;
}
