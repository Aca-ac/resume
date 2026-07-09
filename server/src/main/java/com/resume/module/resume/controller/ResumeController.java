package com.resume.module.resume.controller;

import com.resume.common.Result;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeDetail;
import com.resume.module.resume.entity.ResumeFile;
import com.resume.module.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    // ========== 简历主表 ==========

    @PostMapping
    public Result<Resume> createResume(@RequestAttribute Long userId,
                                       @RequestParam(required = false) String title) {
        return Result.success(resumeService.createResume(userId, title));
    }

    @GetMapping("/{id}")
    public Result<Resume> getResume(@PathVariable Long id, @RequestAttribute Long userId) {
        return Result.success(resumeService.getResume(id, userId));
    }

    @GetMapping
    public Result<List<Resume>> listResumes(@RequestAttribute Long userId) {
        return Result.success(resumeService.listResumes(userId));
    }

    @PutMapping("/{id}")
    public Result<Void> updateTitle(@PathVariable Long id,
                                    @RequestAttribute Long userId,
                                    @RequestParam String title) {
        resumeService.updateResumeTitle(id, userId, title);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteResume(@PathVariable Long id, @RequestAttribute Long userId) {
        resumeService.deleteResume(id, userId);
        return Result.success();
    }

    // ========== 简历明细分段 ==========

    @GetMapping("/{resumeId}/details")
    public Result<List<ResumeDetail>> getDetails(@PathVariable Long resumeId,
                                                 @RequestAttribute Long userId) {
        return Result.success(resumeService.getDetails(resumeId, userId));
    }

    @PostMapping("/{resumeId}/details")
    public Result<ResumeDetail> addDetail(@PathVariable Long resumeId,
                                          @RequestAttribute Long userId,
                                          @RequestParam String sectionType,
                                          @RequestParam String sectionName,
                                          @RequestParam String content,
                                          @RequestParam(required = false) Integer sortOrder) {
        return Result.success(resumeService.addDetail(resumeId, userId, sectionType, sectionName, content, sortOrder));
    }

    @PutMapping("/details/{detailId}")
    public Result<Void> updateDetail(@PathVariable Long detailId,
                                     @RequestAttribute Long userId,
                                     @RequestParam String content) {
        resumeService.updateDetail(detailId, userId, content);
        return Result.success();
    }

    @DeleteMapping("/details/{detailId}")
    public Result<Void> deleteDetail(@PathVariable Long detailId,
                                     @RequestAttribute Long userId) {
        resumeService.deleteDetail(detailId, userId);
        return Result.success();
    }

    // ========== 文件上传与内容提取 ==========

    @PostMapping(value = "/{resumeId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ResumeFile> uploadFile(@PathVariable Long resumeId,
                                         @RequestAttribute Long userId,
                                         @RequestParam("file") MultipartFile file) {
        return Result.success(resumeService.uploadFile(userId, resumeId, file));
    }

    @GetMapping("/files")
    public Result<List<ResumeFile>> listFiles(@RequestAttribute Long userId,
                                              @RequestParam(required = false) Long resumeId) {
        return Result.success(resumeService.listFiles(userId, resumeId));
    }

    /**
     * 提取 PDF/DOCX 文件的文本内容（不调用 OCR，直接解析）
     */
    @PostMapping("/files/{fileId}/extract")
    public Result<ResumeFile> extractFileText(@PathVariable Long fileId,
                                       @RequestAttribute Long userId) {
        return Result.success(resumeService.extractFileText(fileId, userId));
    }

    /**
     * JPG/PNG 图片 OCR 识别（调用通义千问 OCR）
     */
    @PostMapping("/files/{fileId}/ocr")
    public Result<ResumeFile> ocrImage(@PathVariable Long fileId,
                                       @RequestAttribute Long userId) {
        return Result.success(resumeService.ocrImage(fileId, userId));
    }
}
