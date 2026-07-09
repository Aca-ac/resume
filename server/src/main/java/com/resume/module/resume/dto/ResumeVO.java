package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class ResumeVO {
    private Long id;
    private String title;
    private String content;
    private String updatedAt;
}
