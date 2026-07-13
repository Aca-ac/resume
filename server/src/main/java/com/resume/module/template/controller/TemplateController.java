package com.resume.module.template.controller;

import com.resume.common.Result;
import com.resume.module.resume.dto.PageResult;
import com.resume.module.template.dto.TemplateCreateRequest;
import com.resume.module.template.dto.TemplateUploadVO;
import com.resume.module.template.dto.TemplateVO;
import com.resume.module.template.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "简历模板")
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @Operation(summary = "新增模板")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<TemplateVO> create(@Valid @ModelAttribute TemplateCreateRequest request,
                                     @RequestParam(value = "file", required = false) MultipartFile docx,
                                     @RequestParam(value = "preview", required = false) MultipartFile preview) throws IOException {
        return Result.success(templateService.create(request, docx, preview));
    }

    @Operation(summary = "模板列表（分类筛选 + 分页）")
    @GetMapping
    public Result<PageResult<TemplateVO>> list(@RequestParam(required = false) String category,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return Result.success(templateService.list(category, page, size));
    }

    @Operation(summary = "模板详情")
    @GetMapping("/{id}")
    public Result<TemplateVO> detail(@PathVariable Long id) {
        return Result.success(templateService.get(id));
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return Result.success();
    }

    @Operation(summary = "上传模板文件或预览图")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<TemplateUploadVO> upload(@RequestParam(defaultValue = "docx") String type,
                                           @RequestParam("file") MultipartFile file) throws IOException {
        return Result.success(templateService.upload(type, file));
    }
}
