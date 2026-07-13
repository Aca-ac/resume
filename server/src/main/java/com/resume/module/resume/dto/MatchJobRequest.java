package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class MatchJobRequest {
    private Long resumeId;
    private Long jobId;
}