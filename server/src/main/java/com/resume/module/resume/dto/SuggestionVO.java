package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class SuggestionVO {
    private String type;
    private String title;
    private String description;
    private String priority;
    private String category;
}
