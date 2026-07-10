package com.resume.module.resume.controller;

import com.resume.module.resume.dto.*;
import com.resume.module.resume.entity.ResumeFile;
import com.resume.module.resume.service.ResumeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @GetMapping
    public PageResult<ResumeVO> list(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "100") int size) {
        return resumeService.list(resumeService.resolveUserId(authorization), page, size);
    }

    @GetMapping("/{id}")
    public ResumeVO get(@RequestHeader(value = "Authorization", required = false) String authorization,
                        @PathVariable Long id) {
        return resumeService.get(resumeService.resolveUserId(authorization), id);
    }

    @PostMapping
    public ResumeVO create(@RequestHeader(value = "Authorization", required = false) String authorization,
                           @RequestBody ResumeSaveRequest request) {
        return resumeService.create(resumeService.resolveUserId(authorization), request);
    }

    @PutMapping("/{id}")
    public ResumeVO update(@RequestHeader(value = "Authorization", required = false) String authorization,
                           @PathVariable Long id,
                           @RequestBody ResumeSaveRequest request) {
        return resumeService.update(resumeService.resolveUserId(authorization), id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable Long id) {
        resumeService.delete(resumeService.resolveUserId(authorization), id);
        return Map.of("success", true);
    }

    @PostMapping("/import")
    public ImportResultVO importResume(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestParam("file") MultipartFile file) throws IOException {
        return resumeService.importFile(resumeService.resolveUserId(authorization), file);
    }

    @PostMapping("/upload/chunk")
    public ChunkUploadVO uploadChunk(@RequestParam String uploadId,
                                   @RequestParam int chunkIndex,
                                   @RequestParam("file") MultipartFile chunk) throws IOException {
        return resumeService.uploadChunk(uploadId, chunkIndex, chunk);
    }

    @PostMapping("/upload/merge")
    public ImportResultVO mergeChunks(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestParam String uploadId,
                                      @RequestParam String filename,
                                      @RequestParam int totalChunks) throws IOException {
        return resumeService.mergeChunks(resumeService.resolveUserId(authorization), uploadId, filename, totalChunks);
    }

    @PostMapping("/{resumeId}/files")
    public ResumeFile uploadFile(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable Long resumeId,
                                 @RequestParam("file") MultipartFile file) throws IOException {
        return resumeService.uploadFile(resumeService.resolveUserId(authorization), resumeId, file);
    }

    @PostMapping("/files/{fileId}/ocr")
    public OcrResultVO ocr(@RequestHeader(value = "Authorization", required = false) String authorization,
                           @PathVariable Long fileId) {
        return resumeService.runOcr(resumeService.resolveUserId(authorization), fileId);
    }

    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestHeader(value = "Authorization", required = false) String authorization,
                                            @PathVariable Long id) throws IOException {
        return download(resumeService.exportPdf(resumeService.resolveUserId(authorization), id),
                "resume-" + id + ".pdf", MediaType.APPLICATION_PDF);
    }

    @GetMapping("/{id}/export/docx")
    public ResponseEntity<byte[]> exportDocx(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @PathVariable Long id) throws IOException {
        return download(resumeService.exportDocx(resumeService.resolveUserId(authorization), id),
                "resume-" + id + ".docx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    @GetMapping("/{id}/export/text")
    public ResponseEntity<byte[]> exportText(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @PathVariable Long id) {
        return download(resumeService.exportText(resumeService.resolveUserId(authorization), id),
                "resume-" + id + ".txt", MediaType.TEXT_PLAIN);
    }

    private ResponseEntity<byte[]> download(byte[] data, String filename, MediaType mediaType) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(mediaType)
                .body(data);
    }
}
