package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class SpeechRecognizeResultVO {
    private String text;
    private String fileId;
    private String originalFilename;
}
