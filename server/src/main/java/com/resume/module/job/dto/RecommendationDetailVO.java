package com.resume.module.job.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecommendationDetailVO {
    private Long id;
    private Long resumeId;
    private Long jobId;
    private String jobName;
    private String jdContent;
    private Integer matchScore;
    private String matchReason;
    private Integer source;
    private String sourceUrl;
    private Long sourceJobId;
    private Integer hasImprovementSuggestion;
    private ImprovementSuggestionVO improvementSuggestion;
    private LocalDateTime createdAt;
}