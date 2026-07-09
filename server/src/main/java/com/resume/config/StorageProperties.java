package com.resume.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
    private String uploadDir = "uploads/resumes";
    private long maxFileSize = 10 * 1024 * 1024;
}
