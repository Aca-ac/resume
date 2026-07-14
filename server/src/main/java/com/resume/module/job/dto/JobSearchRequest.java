package com.resume.module.job.dto;

import lombok.Data;

@Data
public class JobSearchRequest {
    private String jobName;
    private String existingJd;
}