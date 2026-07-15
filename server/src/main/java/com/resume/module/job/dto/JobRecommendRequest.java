package com.resume.module.job.dto;

import lombok.Data;

@Data
public class JobRecommendRequest {
    private Long resumeId;
    private Boolean enableWebSearch = true;
}