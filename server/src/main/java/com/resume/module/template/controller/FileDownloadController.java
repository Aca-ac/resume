package com.resume.module.template.controller;

import com.resume.module.template.entity.ExportFile;
import com.resume.module.template.service.ExportFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "文件下载")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileDownloadController {

    private final ExportFileService exportFileService;

    @Operation(summary = "下载导出文件")
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> download(@RequestAttribute Long userId,
                                             @PathVariable Long fileId) {
        ExportFile meta = exportFileService.requireOwned(userId, fileId);
        Resource resource = exportFileService.loadForDownload(userId, fileId);
        String encoded = URLEncoder.encode(meta.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mediaType = "PDF".equalsIgnoreCase(meta.getFileType())
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(mediaType)
                .body(resource);
    }
}
