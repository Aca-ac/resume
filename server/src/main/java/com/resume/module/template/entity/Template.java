package com.resume.module.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("templates")
public class Template {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private String previewUrl;
    private String templatePath;
    private String applicableScene;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
