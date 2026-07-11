package com.resume.module.resume.controller;

import com.resume.common.Result;
import com.resume.config.RateLimiter;
import com.resume.module.resume.dto.InterviewAnswerRequest;
import com.resume.module.resume.dto.InterviewMessageVO;
import com.resume.module.resume.dto.InterviewSessionVO;
import com.resume.module.resume.dto.InterviewStartRequest;
import com.resume.module.resume.dto.PageResult;
import com.resume.module.resume.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/start")
    @RateLimiter(key = "interview", maxCount = 20, duration = 60)
    public Result<InterviewSessionVO> start(@RequestAttribute Long userId,
                                            @RequestBody InterviewStartRequest body) {
        return Result.success(interviewService.start(userId, body.getResumeId(), body.getJobTitle()));
    }

    @GetMapping("/{sessionId}/messages")
    public Result<PageResult<InterviewMessageVO>> messages(@RequestAttribute Long userId,
                                                           @PathVariable Long sessionId,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "200") int size) {
        return Result.success(interviewService.listMessages(userId, sessionId, page, size));
    }

    @PostMapping("/{sessionId}/answer")
    @RateLimiter(key = "interview-answer", maxCount = 40, duration = 60)
    public Result<InterviewMessageVO> answer(@RequestAttribute Long userId,
                                             @PathVariable Long sessionId,
                                             @RequestBody InterviewAnswerRequest body) {
        return Result.success(interviewService.answer(userId, sessionId, body.getAnswer()));
    }

    @GetMapping("/{sessionId}/report")
    public Result<InterviewSessionVO> report(@RequestAttribute Long userId,
                                             @PathVariable Long sessionId) {
        return Result.success(interviewService.report(userId, sessionId));
    }
}
