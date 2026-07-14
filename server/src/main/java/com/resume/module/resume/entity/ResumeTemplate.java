package com.resume.module.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历模板表实体（he：V11 迁移与 CRUD；tian 渲染侧通过 TemplateService 读取 template_path）。
 */
@Data
@TableName("resume_templates")
public class ResumeTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private String previewUrl;
    private String templatePath;
    private String applicableScene;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
