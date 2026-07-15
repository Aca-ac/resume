package com.resume.module.job.dto;

import lombok.Data;

@Data
public class JobRecommendationVO {
    private String jobName;
    private String jdContent;
    private Integer matchScore;
    private String matchReason;
    private String source;
    private String sourceUrl;
    private Long sourceJobId;
}