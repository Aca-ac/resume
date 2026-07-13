package com.resume.module.resume.controller;

import com.resume.common.Result;
import com.resume.config.RateLimiter;
import com.resume.module.resume.dto.ChunkUploadVO;
import com.resume.module.resume.dto.ImportResultVO;
import com.resume.module.resume.dto.OptimizeRequest;
import com.resume.module.resume.dto.ResumeSaveRequest;
import com.resume.module.resume.dto.ResumeVO;
import com.resume.module.resume.entity.ResumeDetail;
import com.resume.module.resume.entity.ResumeFile;
import com.resume.module.resume.service.ResumeService;
import com.resume.module.resume.service.TemplateExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final TemplateExportService templateExportService;

    @PostMapping
    public Result<ResumeVO> createResume(@RequestAttribute Long userId,
                                         @RequestBody(required = false) ResumeSaveRequest body,
                                         @RequestParam(required = false) String title) {
        if (body != null && (body.getTitle() != null || body.getContent() != null)) {
            return Result.success(resumeService.createResume(userId, body));
        }
        return Result.success(resumeService.createResume(userId, title));
    }

    @GetMapping("/{id}")
    public Result<ResumeVO> getResume(@PathVariable Long id, @RequestAttribute Long userId) {
        return Result.success(resumeService.getResumeVo(id, userId));
    }

    @GetMapping
    public Result<List<ResumeVO>> listResumes(@RequestAttribute Long userId) {
        return Result.success(resumeService.listResumes(userId));
    }

    @PutMapping("/{id}")
    public Result<ResumeVO> updateResume(@PathVariable Long id,
                                         @RequestAttribute Long userId,
                                         @RequestBody(required = false) ResumeSaveRequest body,
                                         @RequestParam(required = false) String title) {
        if (body != null && (body.getTitle() != null || body.getContent() != null)) {
            return Result.success(resumeService.updateResume(id, userId, body));
        }
        if (title != null) {
            resumeService.updateResumeTitle(id, userId, title);
        }
        return Result.success(resumeService.getResumeVo(id, userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteResume(@PathVariable Long id, @RequestAttribute Long userId) {
        resumeService.deleteResume(id, userId);
        return Result.success();
    }

    @PostMapping("/{id}/optimize")
    @RateLimiter(key = "optimize", maxCount = 20, duration = 60)
    public Result<ResumeVO> optimizeResume(@PathVariable Long id,
                                           @RequestAttribute Long userId,
                                           @RequestBody(required = false) OptimizeRequest body) {
        String targetRole = body == null ? null : body.getTargetRole();
        return Result.success(resumeService.optimizeResume(userId, id, targetRole));
    }

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

    @PostMapping("/import")
    public Result<ImportResultVO> importResume(@RequestAttribute Long userId,
                                               @RequestParam("file") MultipartFile file) throws IOException {
        return Result.success(resumeService.importFile(userId, file));
    }

    @PostMapping("/upload/chunk")
    public Result<ChunkUploadVO> uploadChunk(@RequestParam String uploadId,
                                             @RequestParam int chunkIndex,
                                             @RequestParam("file") MultipartFile chunk) throws IOException {
        return Result.success(resumeService.uploadChunk(uploadId, chunkIndex, chunk));
    }

    @PostMapping("/upload/merge")
    public Result<ImportResultVO> mergeChunks(@RequestAttribute Long userId,
                                              @RequestParam String uploadId,
                                              @RequestParam String filename,
                                              @RequestParam int totalChunks) throws IOException {
        return Result.success(resumeService.mergeChunks(userId, uploadId, filename, totalChunks));
    }

    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id,
                                            @RequestAttribute Long userId,
                                            @RequestParam(required = false) Long templateId) throws IOException {
        byte[] data = templateId != null
                ? templateExportService.exportPdf(userId, id, templateId)
                : resumeService.exportPdf(userId, id);
        return download(data, "resume-" + id + ".pdf", MediaType.APPLICATION_PDF);
    }

    @GetMapping("/{id}/export/word")
    public ResponseEntity<byte[]> exportWordByTemplate(@PathVariable Long id,
                                                       @RequestAttribute Long userId,
                                                       @RequestParam Long templateId) throws IOException {
        byte[] data = templateExportService.exportWord(userId, id, templateId);
        return download(data, "resume-" + id + ".docx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    @GetMapping("/{id}/export/docx")
    public ResponseEntity<byte[]> exportDocx(@PathVariable Long id,
                                             @RequestAttribute Long userId) throws IOException {
        return download(resumeService.exportDocx(userId, id), "resume-" + id + ".docx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    @GetMapping("/{id}/export/text")
    public ResponseEntity<byte[]> exportText(@PathVariable Long id,
                                             @RequestAttribute Long userId) {
        return download(resumeService.exportText(userId, id), "resume-" + id + ".txt", MediaType.TEXT_PLAIN);
    }

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

    @PostMapping("/files/{fileId}/extract")
    public Result<ResumeFile> extractFileText(@PathVariable Long fileId,
                                             @RequestAttribute Long userId) {
        return Result.success(resumeService.extractFileText(fileId, userId));
    }

    @PostMapping("/files/{fileId}/ocr")
    public Result<ResumeFile> ocrImage(@PathVariable Long fileId,
                                       @RequestAttribute Long userId) {
        return Result.success(resumeService.ocrImage(fileId, userId));
    }

    private ResponseEntity<byte[]> download(byte[] data, String filename, MediaType mediaType) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(mediaType)
                .body(data);
    }
}
