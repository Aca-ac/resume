package com.resume.module.resume.controller;

import com.resume.common.Result;
import com.resume.config.RateLimiter;
import com.resume.module.resume.dto.AnalysisResult;
import com.resume.module.resume.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 简历智能分析接口。
 */
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    /**
     * 对指定简历发起 AI 多维度分析，返回评分与优化建议。
     * 限流：同一用户每分钟最多 20 次。
     */
    @PostMapping("/resume/{resumeId}")
    @RateLimiter(key = "analysis", maxCount = 20, duration = 60)
    public Result<AnalysisResult> analyzeResume(@PathVariable Long resumeId, @RequestAttribute Long userId) {
        return Result.success(analysisService.analyzeResume(userId, resumeId));
    }

    /**
     * 分页查询当前用户的分析历史记录。
     */
    @GetMapping("/records")
    public Result<Map<String, Object>> listRecords(@RequestAttribute Long userId,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return Result.success(analysisService.listRecords(userId, page, size));
    }

    /**
     * 查询单条分析记录详情。
     */
    @GetMapping("/records/{recordId}")
    public Result<AnalysisResult> getRecord(@PathVariable Long recordId, @RequestAttribute Long userId) {
        return Result.success(analysisService.getRecord(recordId, userId));
    }
}
