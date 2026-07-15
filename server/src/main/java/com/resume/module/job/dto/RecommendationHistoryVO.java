package com.resume.module.job.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecommendationHistoryVO {
    private Long id;
    private Long resumeId;
    private String jobName;
    private Integer matchScore;
    private Integer source;
    private LocalDateTime createdAt;
}