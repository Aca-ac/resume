package com.resume.module.job.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.Result;
import com.resume.config.RateLimiter;
import com.resume.module.job.dto.JobRecommendRequest;
import com.resume.module.job.dto.PageResult;
import com.resume.module.job.dto.RecommendationDetailVO;
import com.resume.module.job.dto.RecommendationHistoryVO;
import com.resume.module.job.dto.RecommendationResultVO;
import com.resume.module.job.service.FindJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/match")
@RequiredArgsConstructor
@Slf4j
public class FindJobController {

    private final FindJobService findJobService;
    private final ObjectMapper objectMapper;

    @PostMapping("/recommend")
    @RateLimiter(key = "job_recommend", maxCount = 5, duration = 60)
    public Result<RecommendationResultVO> recommendJobs(@RequestAttribute Long userId,
                                                        @RequestBody JobRecommendRequest body) {
        log.info("接收到岗位匹配推荐请求，userId={}, resumeId={}, enableWebSearch={}", userId, body.getResumeId(), body.getEnableWebSearch());
        long startTime = System.currentTimeMillis();
        
        Boolean enableWebSearch = body.getEnableWebSearch();
        if (enableWebSearch == null) {
            enableWebSearch = true;
        }
        RecommendationResultVO result = findJobService.recommendJobs(userId, body.getResumeId(), enableWebSearch);
        long duration = System.currentTimeMillis() - startTime;
        
        try {
            log.info("岗位匹配推荐完成，总用时{}ms，返回结果: {}", duration, objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.info("岗位匹配推荐完成，总用时{}ms", duration);
        }
        
        return Result.success(result);
    }

    @GetMapping("/recommendations")
    public Result<PageResult<RecommendationHistoryVO>> listRecommendations(@RequestAttribute Long userId,
                                                                           @RequestParam(defaultValue = "1") int page,
                                                                           @RequestParam(defaultValue = "20") int size) {
        return Result.success(findJobService.listRecommendations(userId, page, size));
    }

    @GetMapping("/recommendations/{id}")
    public Result<RecommendationDetailVO> getRecommendationDetail(@RequestAttribute Long userId,
                                                                  @PathVariable Long id) {
        return Result.success(findJobService.getRecommendationDetail(userId, id));
    }
}