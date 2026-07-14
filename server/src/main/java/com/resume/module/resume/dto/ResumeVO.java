package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class ResumeVO {
    private Long id;
    private String title;
    private String sourceType;
    private String content;
    /** 有照片时：/api/v1/resumes/{id}/photo */
    private String photoUrl;
    private String updatedAt;
}
