package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class MatchRecordVO {
    private Long id;
    private Long resumeId;
    private Integer matchScore;
    private Integer summaryScore;
    private Integer educationScore;
    private Integer experienceScore;
    private Integer skillScore;
    private Integer projectScore;
    private String analysis;
    private String createdAt;
}
