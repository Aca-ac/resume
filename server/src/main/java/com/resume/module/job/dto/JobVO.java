package com.resume.module.job.dto;

import lombok.Data;

import java.util.List;

@Data
public class JobVO {
    private Long id;
    private String jobName;
    private String jdContent;
    private Integer source;
    private Long originalJobId;
    private List<String> sourceUrls;
    private String createdAt;
    private String updatedAt;
    private OwnerVO owner;
}