package com.resume.module.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resumes")
public class Resume {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    /** MANUAL=新建 IMPORT=导入 */
    private String sourceType;
    private String content;
    /** 每次保存 +1 */
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
