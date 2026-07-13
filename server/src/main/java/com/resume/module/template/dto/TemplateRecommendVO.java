package com.resume.module.template.dto;

import lombok.Data;

@Data
public class TemplateRecommendVO {
    private TemplateVO template;
    private int score;
    private String reason;
}
