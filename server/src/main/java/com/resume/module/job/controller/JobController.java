package com.resume.module.job.controller;

import com.resume.common.Result;
import com.resume.module.job.dto.JobCreateRequest;
import com.resume.module.job.dto.JobVO;
import com.resume.module.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public Result<JobVO> createJob(@RequestAttribute Long userId,
                                   @RequestBody JobCreateRequest body) {
        return Result.success(jobService.createJob(userId, body));
    }
}