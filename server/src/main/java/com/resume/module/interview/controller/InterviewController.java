package com.resume.module.interview.controller;

import com.resume.common.BaseController;
import com.resume.common.PageResult;
import com.resume.common.Result;
import com.resume.module.interview.entity.InterviewMessage;
import com.resume.module.interview.entity.InterviewSession;
import com.resume.module.interview.service.InterviewSessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/interview")
@RequiredArgsConstructor
public class InterviewController extends BaseController {

    private final InterviewSessionService interviewSessionService;

    @PostMapping("/start")
    public Result<InterviewSession> start(@Valid @RequestBody StartRequest request, Authentication authentication) {
        return Result.ok(interviewSessionService.start(userId(authentication), request.getResumeId(), request.getJobTitle()));
    }

    @GetMapping("/{sessionId}/messages")
    public Result<PageResult<InterviewMessage>> messages(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size,
            Authentication authentication) {
        return Result.ok(PageResult.of(interviewSessionService.messages(userId(authentication), sessionId, page, size)));
    }

    @PostMapping("/{sessionId}/answer")
    public Result<InterviewMessage> answer(@PathVariable Long sessionId, @Valid @RequestBody AnswerRequest request, Authentication authentication) {
        return Result.ok(interviewSessionService.answer(userId(authentication), sessionId, request.getAnswer()));
    }

    @GetMapping(value = "/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long sessionId, Authentication authentication) {
        return interviewSessionService.stream(userId(authentication), sessionId);
    }

    @GetMapping("/{sessionId}/report")
    public Result<InterviewSession> report(@PathVariable Long sessionId, Authentication authentication) {
        return Result.ok(interviewSessionService.report(userId(authentication), sessionId));
    }

    @Data
    public static class StartRequest {
        @NotNull
        private Long resumeId;
        @NotBlank
        @Size(max = 200)
        private String jobTitle;
    }

    @Data
    public static class AnswerRequest {
        @NotBlank
        @Size(max = 10000)
        private String answer;
    }
}