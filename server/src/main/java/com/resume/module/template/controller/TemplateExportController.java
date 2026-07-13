package com.resume.module.template.controller;

import com.resume.common.Result;
import com.resume.module.template.dto.ExportResultVO;
import com.resume.module.template.dto.TemplateExportRequest;
import com.resume.module.template.service.ExportFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Tag(name = "模板化简历导出")
@RestController
@RequestMapping("/api/v1/resumes/export")
@RequiredArgsConstructor
public class TemplateExportController {

    private final ExportFileService exportFileService;

    @Operation(summary = "按模板导出 Word")
    @PostMapping("/word")
    public Result<ExportResultVO> exportWord(@RequestAttribute Long userId,
                                             @Valid @RequestBody TemplateExportRequest request) throws IOException {
        return Result.success(exportFileService.exportWord(userId, request));
    }

    @Operation(summary = "按模板导出 PDF（结构化文本 + CJK 字体）")
    @PostMapping("/pdf")
    public Result<ExportResultVO> exportPdf(@RequestAttribute Long userId,
                                            @Valid @RequestBody TemplateExportRequest request) throws IOException {
        return Result.success(exportFileService.exportPdf(userId, request));
    }
}
