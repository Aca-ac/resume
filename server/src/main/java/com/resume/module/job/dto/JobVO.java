package com.resume.module.job.dto;

import lombok.Data;

@Data
public class JobVO {
    private Long id;
    private String jobName;
    private String jdContent;
    private Integer source;
    private Long originalJobId;
    private String createdAt;
    private String updatedAt;
}