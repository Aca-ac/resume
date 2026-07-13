package com.resume.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.export")
public class ExportProperties {
    /** LibreOffice 可执行命令，默认 soffice（需在 PATH 中） */
    private String libreofficeCommand = "soffice";
    private int convertTimeoutSeconds = 120;
    /** 导出文件临时目录（相对或绝对路径） */
    private String exportDir = "uploads/exports";
    /** 临时导出文件保留小时数 */
    private int fileTtlHours = 24;
}
