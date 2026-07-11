package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class MatchRecordVO {
    private Long id;
    private Long resumeId;
    private Integer matchScore;
    private String analysis;
    private String createdAt;
}
