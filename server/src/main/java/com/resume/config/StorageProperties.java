package com.resume.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
    private String uploadDir = "uploads/resumes";
    private long maxFileSize = 100 * 1024 * 1024;
    private String chunkDir = "uploads/resumes/chunks";
    /** Word 模板与预览图根目录 */
    private String templateDir = "uploads/templates";
    /** 临时导出文件目录 */
    private String exportDir = "uploads/exports";
    /** 导出文件保留小时数 */
    private int exportTtlHours = 24;
}
