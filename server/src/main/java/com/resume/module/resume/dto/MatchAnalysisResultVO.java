package com.resume.module.resume.dto;

import lombok.Data;

import java.util.List;

@Data
public class MatchAnalysisResultVO {
    private Long id;
    private Long resumeId;
    private Long jobDescriptionId;
    private Integer matchScore;
    private String matchLevel;
    private List<DimensionScoreVO> dimensions;
    private String analysis;
    private String summary;
    private List<String> highlights;
    private List<String> weaknesses;
    private List<SuggestionVO> suggestions;
    private String createdAt;
    private String updatedAt;
    private String version;
}
