package com.resume.module.resume.controller;

import com.resume.common.Result;
import com.resume.config.RateLimiter;
import com.resume.module.resume.dto.InterviewAnswerRequest;
import com.resume.module.resume.dto.InterviewAnswerResultVO;
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
        return Result.success(interviewService.start(
                userId,
                body.getResumeId(),
                body.getJobTitle(),
                body.getJobId(),
                body.getMaxQuestions()));
    }

    @GetMapping("/history")
    public Result<PageResult<InterviewSessionVO>> history(@RequestAttribute Long userId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return Result.success(interviewService.history(userId, page, size));
    }

    @GetMapping("/{sessionId}")
    public Result<InterviewSessionVO> detail(@RequestAttribute Long userId,
                                             @PathVariable Long sessionId) {
        return Result.success(interviewService.getSession(userId, sessionId));
    }

    @GetMapping("/{sessionId}/messages")
    public Result<PageResult<InterviewMessageVO>> messages(@RequestAttribute Long userId,
                                                           @PathVariable Long sessionId,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "200") int size,
                                                           @RequestParam(required = false) Integer afterSeq) {
        return Result.success(interviewService.listMessages(userId, sessionId, page, size, afterSeq));
    }

    @PostMapping("/{sessionId}/answer")
    @RateLimiter(key = "interview-answer", maxCount = 40, duration = 60)
    public Result<InterviewAnswerResultVO> answer(@RequestAttribute Long userId,
                                                  @PathVariable Long sessionId,
                                                  @RequestBody InterviewAnswerRequest body) {
        return Result.success(interviewService.answer(
                userId, sessionId, body.getAnswer(), body.getClientMsgId()));
    }

    /**
     * 单独触发生成下一题（状态须为 QUESTIONING，且上一题已答完后可用于补题；
     * 一般流程由 /answer 自动生成，本接口供前端重试或调试）。
     */
    @PostMapping("/{sessionId}/question")
    @RateLimiter(key = "interview-question", maxCount = 20, duration = 60)
    public Result<InterviewMessageVO> nextQuestion(@RequestAttribute Long userId,
                                                   @PathVariable Long sessionId) {
        return Result.success(interviewService.generateNextQuestion(userId, sessionId));
    }

    @PostMapping("/{sessionId}/end")
    public Result<InterviewSessionVO> end(@RequestAttribute Long userId,
                                          @PathVariable Long sessionId) {
        return Result.success(interviewService.end(userId, sessionId));
    }

    @GetMapping("/{sessionId}/report")
    public Result<InterviewSessionVO> report(@RequestAttribute Long userId,
                                             @PathVariable Long sessionId) {
        return Result.success(interviewService.report(userId, sessionId));
    }
}
