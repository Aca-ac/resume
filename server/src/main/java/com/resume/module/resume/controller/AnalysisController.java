package com.resume.module.resume.controller;

import com.resume.common.Result;
import com.resume.config.RateLimiter;
import com.resume.module.resume.dto.AnalysisResult;
import com.resume.module.resume.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {
    private final AnalysisService analysisService;

    @PostMapping("/resume/{resumeId}")
    @RateLimiter(key = "analysis", maxCount = 20, duration = 60)
    public Result<AnalysisResult> analyzeResume(@PathVariable Long resumeId, @RequestAttribute Long userId) {
        return Result.success(analysisService.analyzeResume(userId, resumeId));
    }

    @GetMapping("/records")
    public Result<Map<String, Object>> listRecords(@RequestAttribute Long userId,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return Result.success(analysisService.listRecords(userId, page, size));
    }

    @GetMapping("/records/{recordId}")
    public Result<AnalysisResult> getRecord(@PathVariable Long recordId, @RequestAttribute Long userId) {
        return Result.success(analysisService.getRecord(recordId, userId));
    }
}