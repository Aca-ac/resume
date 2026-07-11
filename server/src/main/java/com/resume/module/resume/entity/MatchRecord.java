package com.resume.module.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("match_records")
public class MatchRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long resumeId;
    private String jdText;
    private Integer matchScore;
    private Integer summaryScore;
    private Integer educationScore;
    private Integer experienceScore;
    private Integer skillScore;
    private Integer projectScore;
    private String analysis;
    private LocalDateTime createdAt;
}
