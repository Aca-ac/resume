package com.resume.module.job.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RecommendationResultVO {
    private Long resumeId;
    private List<JobRecommendationVO> recommendations;
    private ImprovementSuggestionVO improvementSuggestion;
    private LocalDateTime createdAt;
}