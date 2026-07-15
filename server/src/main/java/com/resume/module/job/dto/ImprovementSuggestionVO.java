package com.resume.module.job.dto;

import lombok.Data;

import java.util.List;

@Data
public class ImprovementSuggestionVO {
    private String problemDescription;
    private List<String> suggestions;
    private String priority;
}