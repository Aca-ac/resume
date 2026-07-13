package com.resume.module.resume.dto;

import lombok.Builder;
import lombok.Data;

/**
 * poi-tl 模板占位符数据（用户内容填充，不含模板名）。
 */
@Data
@Builder
public class TemplateRenderData {
    /** {{name}} 姓名 */
    private String name;
    /** {{phone}} 手机 */
    private String phone;
    /** {{email}} 邮箱 */
    private String email;
    /** {{education}} 教育经历，section_type=EDUCATION */
    private String education;
    /** {{workExperience}} 工作经历，section_type=WORK_EXPERIENCE */
    private String workExperience;
    /** {{project}} 项目经历，section_type=PROJECT */
    private String project;
    /** {{skill}} 技能证书，section_type=SKILL */
    private String skill;
    /** {{summary}} 个人总结，section_type=SUMMARY */
    private String summary;
}
