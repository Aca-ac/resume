package com.resume.module.resume.dto;

import lombok.Data;
import java.util.Map;

@Data
public class AnalysisResult {
    private Long id;
    private Long resumeId;
    private String resumeTitle;
    private Map<String, Integer> scores;
    private Integer totalScore;
    private String suggestions;
    private String status;
    private String createdAt;
}