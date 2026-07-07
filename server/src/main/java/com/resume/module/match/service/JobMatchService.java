package com.resume.module.match.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.ai.AIServiceFacade;
import com.resume.module.match.entity.MatchRecord;
import com.resume.module.match.mapper.MatchRecordMapper;
import com.resume.module.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JobMatchService {

    private final MatchRecordMapper matchRecordMapper;
    private final ResumeService resumeService;
    private final AIServiceFacade aiServiceFacade;

    @Transactional(rollbackFor = Exception.class)
    public MatchRecord matchJd(Long userId, Long resumeId, String jdText) {
        String resumeContent = resumeService.getOwned(userId, resumeId).getContent();
        Map<String, Object> result = aiServiceFacade.matchJob(userId, resumeContent, jdText);
        MatchRecord record = new MatchRecord();
        record.setUserId(userId);
        record.setResumeId(resumeId);
        record.setJdText(jdText);
        record.setMatchScore((Integer) result.getOrDefault("score", 0));
        record.setAnalysis(String.valueOf(result.getOrDefault("analysis", "")));
        record.setCreatedAt(LocalDateTime.now());
        matchRecordMapper.insert(record);
        return record;
    }

    public List<MatchRecord> history(Long userId) {
        return matchRecordMapper.selectList(new LambdaQueryWrapper<MatchRecord>()
                .eq(MatchRecord::getUserId, userId)
                .orderByDesc(MatchRecord::getCreatedAt));
    }

    public Page<MatchRecord> history(Long userId, int page, int size) {
        return matchRecordMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<MatchRecord>()
                .eq(MatchRecord::getUserId, userId)
                .orderByDesc(MatchRecord::getCreatedAt));
    }
}
