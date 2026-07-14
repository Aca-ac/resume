package com.resume.module.job.dto;

import lombok.Data;

import java.util.List;

@Data
public class JobSearchResultVO {
    private String jobName;
    private String jdContent;
    private List<String> sources;
    private Boolean webSearchSuccess;
}