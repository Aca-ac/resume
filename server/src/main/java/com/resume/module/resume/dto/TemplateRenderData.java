package com.resume.module.resume.dto;

import lombok.Builder;
import lombok.Data;

/**
 * poi-tl 模板占位符数据，字段名与模版实现落地方案.md 约定一致。
 */
@Data
@Builder
public class TemplateRenderData {
    private String title;
    private String name;
    private String phone;
    private String email;
    private String summary;
    private String education;
    private String workExperience;
    private String project;
    private String skill;
}
