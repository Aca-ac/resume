package com.resume.module.template.task;

import com.resume.module.template.service.ExportFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportFileCleanTask {

    private final ExportFileService exportFileService;

    /** 每小时清理过期导出文件 */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanExpiredExports() {
        try {
            int n = exportFileService.cleanExpired();
            if (n > 0) {
                log.info("ExportFileCleanTask removed {} files", n);
            }
        } catch (Exception e) {
            log.warn("ExportFileCleanTask failed: {}", e.getMessage());
        }
    }
}
