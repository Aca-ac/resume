package com.resume.module.job.service;

import com.resume.common.BusinessException;
import com.resume.module.job.dto.JobCreateRequest;
import com.resume.module.job.dto.JobVO;
import com.resume.module.job.entity.TargetJob;
import com.resume.module.job.mapper.TargetJobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class JobService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Integer SOURCE_MANUAL = 0;

    private final TargetJobMapper targetJobMapper;

    @Transactional
    public JobVO createJob(Long userId, JobCreateRequest request) {
        if (request.getJobName() == null || request.getJobName().isBlank()) {
            throw new BusinessException(400, "岗位名称不能为空");
        }

        TargetJob job = new TargetJob();
        job.setUserId(userId);
        job.setJobName(request.getJobName().trim());
        job.setJdContent(request.getJdContent());
        job.setSource(SOURCE_MANUAL);

        targetJobMapper.insert(job);

        return toVO(job);
    }

    private JobVO toVO(TargetJob job) {
        JobVO vo = new JobVO();
        vo.setId(job.getId());
        vo.setJobName(job.getJobName());
        vo.setJdContent(job.getJdContent());
        vo.setSource(job.getSource());
        vo.setOriginalJobId(job.getOriginalJobId());
        vo.setCreatedAt(job.getCreatedAt() == null ? null : job.getCreatedAt().format(FMT));
        vo.setUpdatedAt(job.getUpdatedAt() == null ? null : job.getUpdatedAt().format(FMT));
        return vo;
    }
}