package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class InterviewSessionVO {
    private Long id;
    private Long resumeId;
    private Long jobId;
    private String jobTitle;
    private String status;
    private String state;
    private Integer questionIndex;
    private Integer maxQuestions;
    private Integer lastSeq;
    private String report;
    private String createdAt;
    private String endedAt;
    /** start 接口附带首题，便于前端少一次拉取 */
    private InterviewMessageVO firstQuestion;
}
