package com.resume.module.resume.controller;

import com.resume.common.Result;
import com.resume.config.RateLimiter;
import com.resume.module.resume.dto.MatchJdRequest;
import com.resume.module.resume.dto.MatchRecordVO;
import com.resume.module.resume.dto.PageResult;
import com.resume.module.resume.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/match")
@RequiredArgsConstructor
@Slf4j
public class MatchController {

    private final MatchService matchService;

    @PostMapping("/jd")
    @RateLimiter(key = "match", maxCount = 20, duration = 60)
    public Result<MatchRecordVO> matchJd(@RequestAttribute Long userId,
                                         @RequestBody MatchJdRequest body) {
        log.info("接收到匹配请求，userId={}", userId);
        return Result.success(matchService.matchJd(userId, body.getResumeId(), body.getJdText()));
    }

    @GetMapping("/history")
    public Result<PageResult<MatchRecordVO>> history(@RequestAttribute Long userId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "50") int size) {
        return Result.success(matchService.listHistory(userId, page, size));
    }
}
