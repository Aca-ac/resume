package com.resume.module.job.dto;

import lombok.Data;

import java.util.List;

@Data
public class JobSimpleVO {
    private Long id;
    private String jobName;
    private Integer source;
    private List<String> sourceUrls;
    private String createdAt;
}
