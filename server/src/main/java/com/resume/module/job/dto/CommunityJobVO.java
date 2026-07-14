package com.resume.module.job.dto;

import lombok.Data;

@Data
public class CommunityJobVO {
    private Long id;
    private String jobName;
    private Integer source;
    private String createdAt;
    private OwnerVO owner;
}