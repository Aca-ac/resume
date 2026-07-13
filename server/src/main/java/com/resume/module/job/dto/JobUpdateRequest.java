package com.resume.module.job.dto;

import lombok.Data;

@Data
public class JobUpdateRequest {
    private String jobName;
    private String jdContent;
}