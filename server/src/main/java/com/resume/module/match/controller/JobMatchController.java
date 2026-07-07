package com.resume.module.match.controller;

import com.resume.common.BaseController;
import com.resume.common.PageResult;
import com.resume.common.Result;
import com.resume.module.match.entity.MatchRecord;
import com.resume.module.match.service.JobMatchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/match")
@RequiredArgsConstructor
public class JobMatchController extends BaseController {

    private final JobMatchService jobMatchService;

    @PostMapping("/jd")
    public Result<MatchRecord> match(@Valid @RequestBody MatchRequest request, Authentication authentication) {
        return Result.ok(jobMatchService.matchJd(userId(authentication), request.getResumeId(), request.getJdText()));
    }

    @GetMapping("/history")
    public Result<PageResult<MatchRecord>> history(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication) {
        return Result.ok(PageResult.of(jobMatchService.history(userId(authentication), page, size)));
    }

    @Data
    public static class MatchRequest {
        @NotNull
        private Long resumeId;
        @NotBlank
        @Size(max = 51200)
        private String jdText;
    }
}