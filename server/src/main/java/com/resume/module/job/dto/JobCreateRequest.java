package com.resume.module.job.dto;

import lombok.Data;

@Data
public class JobCreateRequest {
    private String jobName;
    private String jdContent;
}