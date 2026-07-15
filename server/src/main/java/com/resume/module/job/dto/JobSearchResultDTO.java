package com.resume.module.job.dto;

import lombok.Data;

@Data
public class JobSearchResultDTO {
    private String jobName;
    private String jdContent;
    private Integer summary_score;
    private Integer education_score;
    private Integer experience_score;
    private Integer skill_score;
    private Integer project_score;
    private String matchReason;
    private String source;
    private String sourceUrl;
}