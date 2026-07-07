package com.resume.module.resume.controller;

import com.resume.common.BaseController;
import com.resume.common.PageResult;
import com.resume.common.Result;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.service.ResumeOptimizeService;
import com.resume.module.resume.service.ResumePdfService;
import com.resume.module.resume.service.ResumeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController extends BaseController {

    private final ResumeService resumeService;
    private final ResumeOptimizeService resumeOptimizeService;
    private final ResumePdfService resumePdfService;

    @GetMapping
    public Result<PageResult<Resume>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication) {
        return Result.ok(PageResult.of(resumeService.listByUser(userId(authentication), page, size)));
    }

    @GetMapping("/{id}")
    public Result<Resume> get(@PathVariable Long id, Authentication authentication) {
        return Result.ok(resumeService.getOwned(userId(authentication), id));
    }

    @PostMapping
    public Result<Resume> create(@Valid @RequestBody ResumeRequest request, Authentication authentication) {
        return Result.ok(resumeService.create(userId(authentication), request.getTitle(), request.getContent()));
    }

    @PutMapping("/{id}")
    public Result<Resume> update(@PathVariable Long id, @Valid @RequestBody ResumeRequest request, Authentication authentication) {
        return Result.ok(resumeService.update(userId(authentication), id, request.getTitle(), request.getContent()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        resumeService.delete(userId(authentication), id);
        return Result.ok();
    }

    @PostMapping("/{id}/optimize")
    public Result<Resume> optimize(@PathVariable Long id, @Valid @RequestBody OptimizeRequest request, Authentication authentication) {
        return Result.ok(resumeOptimizeService.optimize(userId(authentication), id, request.getTargetRole()));
    }

    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id, Authentication authentication) throws Exception {
        Resume resume = resumeService.getOwned(userId(authentication), id);
        byte[] pdf = resumePdfService.export(resume.getTitle(), resume.getContent());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resume-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/export/text")
    public ResponseEntity<byte[]> exportText(@PathVariable Long id, Authentication authentication) {
        Resume resume = resumeService.getOwned(userId(authentication), id);
        String body = resume.getTitle() + "\n\n" + resume.getContent();
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resume-" + id + ".txt")
                .contentType(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))
                .body(bytes);
    }

    @Data
    public static class ResumeRequest {
        @NotBlank
        @Size(max = 200)
        private String title;
        @NotBlank
        @Size(max = 51200)
        private String content;
    }

    @Data
    public static class OptimizeRequest {
        @Size(max = 200)
        private String targetRole = "Software Engineer";
    }
}