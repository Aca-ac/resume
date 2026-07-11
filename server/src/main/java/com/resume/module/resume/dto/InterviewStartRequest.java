package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class InterviewStartRequest {
    private Long resumeId;
    private String jobTitle;
}
