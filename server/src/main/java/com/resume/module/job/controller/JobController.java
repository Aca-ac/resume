package com.resume.module.job.controller;

import com.resume.common.Result;
import com.resume.config.RateLimiter;
import com.resume.module.job.dto.CommentCreateRequest;
import com.resume.module.job.dto.CommentVO;
import com.resume.module.job.dto.JobCreateRequest;
import com.resume.module.job.dto.JobSearchRequest;
import com.resume.module.job.dto.CommunityJobVO;
import com.resume.module.job.dto.JobSearchResultVO;
import com.resume.module.job.dto.JobSimpleVO;
import com.resume.module.job.dto.JobUpdateRequest;
import com.resume.module.job.dto.JobVO;
import com.resume.module.job.dto.PageResult;
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

    @GetMapping
    public Result<PageResult<JobSimpleVO>> listJobs(@RequestAttribute Long userId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return Result.success(jobService.listJobs(userId, page, size));
    }

    @GetMapping("/{id}")
    public Result<JobVO> getJob(@RequestAttribute Long userId,
                                @PathVariable Long id) {
        return Result.success(jobService.getJob(userId, id));
    }

    @PutMapping("/{id}")
    public Result<JobVO> updateJob(@RequestAttribute Long userId,
                                   @PathVariable Long id,
                                   @RequestBody JobUpdateRequest body) {
        return Result.success(jobService.updateJob(userId, id, body));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteJob(@RequestAttribute Long userId,
                                  @PathVariable Long id) {
        jobService.deleteJob(userId, id);
        return Result.success();
    }

    @PostMapping("/search")
    @RateLimiter(key = "job_search", maxCount = 5, duration = 60)
    public Result<JobSearchResultVO> searchJob(@RequestAttribute Long userId,
                                               @RequestBody JobSearchRequest body) {
        return Result.success(jobService.searchJob(userId, body));
    }

    @GetMapping("/community")
    public Result<PageResult<CommunityJobVO>> listCommunity(@RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return Result.success(jobService.listCommunity(page, size));
    }

    @GetMapping("/community/search")
    public Result<PageResult<CommunityJobVO>> searchCommunity(@RequestParam String keyword,
                                                               @RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return Result.success(jobService.searchCommunity(keyword, page, size));
    }

    @PostMapping("/{id}/fork")
    public Result<JobVO> forkJob(@RequestAttribute Long userId,
                                 @PathVariable Long id) {
        return Result.success(jobService.forkJob(userId, id));
    }

    @GetMapping("/{id}/comments")
    public Result<PageResult<CommentVO>> listComments(@RequestAttribute Long userId,
                                                      @PathVariable Long id,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return Result.success(jobService.listComments(userId, id, page, size));
    }

    @PostMapping("/{id}/comments")
    public Result<CommentVO> createComment(@RequestAttribute Long userId,
                                           @PathVariable Long id,
                                           @RequestBody CommentCreateRequest body) {
        return Result.success(jobService.createComment(userId, id, body));
    }
}