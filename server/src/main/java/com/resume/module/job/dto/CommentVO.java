package com.resume.module.job.dto;

import lombok.Data;

@Data
public class CommentVO {
    private Long id;
    private Long jobId;
    private String content;
    private String createdAt;
    private OwnerVO user;
}