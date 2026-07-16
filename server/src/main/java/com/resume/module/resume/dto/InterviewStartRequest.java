package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class InterviewStartRequest {
    private Long resumeId;
    private String jobTitle;
    /** 可选：绑定目标岗位，用于拉取 JD 快照 */
    private Long jobId;
    /** 可选：默认 5，范围 3–10 */
    private Integer maxQuestions;
}
