package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class InterviewMessageVO {
    private Long id;
    private String role;
    private String messageType;
    private Integer questionIndex;
    private String content;
    private String evaluation;
    private Integer seq;
    private String createdAt;
}
