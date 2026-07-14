package com.resume.module.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("match_analysis")
public class MatchAnalysis {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long resumeId;
    private Long jobDescriptionId;
    private String jdText;
    private Integer matchScore;
    private String matchLevel;
    private String analysis;
    private String summary;
    /** JSON array string */
    private String highlights;
    /** JSON array string */
    private String weaknesses;
    /** JSON array string */
    private String suggestions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
