package com.resume.module.resume.service;

import com.resume.ai.AIServiceFacade;
import com.resume.module.resume.entity.Resume;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResumeOptimizeService {

    private final ResumeService resumeService;
    private final AIServiceFacade aiServiceFacade;

    @Transactional(rollbackFor = Exception.class)
    public Resume optimize(Long userId, Long resumeId, String targetRole) {
        Resume resume = resumeService.getOwned(userId, resumeId);
        String optimized = aiServiceFacade.optimizeResume(userId, resume.getContent(), targetRole);
        return resumeService.applyOptimizedContent(resume, optimized);
    }
}
