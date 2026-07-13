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
}
