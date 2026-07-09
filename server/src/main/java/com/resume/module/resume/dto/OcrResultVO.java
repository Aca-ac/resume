package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class OcrResultVO {
    private Long fileId;
    private String ocrText;
}
