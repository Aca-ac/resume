package com.resume.module.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.BusinessException;
import com.resume.common.ErrorCode;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeVersion;
import com.resume.module.resume.mapper.ResumeMapper;
import com.resume.module.resume.mapper.ResumeVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeMapper resumeMapper;
    private final ResumeVersionMapper resumeVersionMapper;

    public List<Resume> listByUser(Long userId) {
        return resumeMapper.selectList(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .orderByDesc(Resume::getUpdatedAt));
    }

    public Page<Resume> listByUser(Long userId, int page, int size) {
        return resumeMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .orderByDesc(Resume::getUpdatedAt));
    }

    public Resume getOwned(Long userId, Long id) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "resume not found");
        }
        return resume;
    }

    @Transactional(rollbackFor = Exception.class)
    public Resume create(Long userId, String title, String content) {
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(title);
        resume.setContent(content);
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(resume);
        saveVersion(resume.getId(), content, 1);
        return resume;
    }

    @Transactional(rollbackFor = Exception.class)
    public Resume update(Long userId, Long id, String title, String content) {
        Resume resume = getOwned(userId, id);
        resume.setTitle(title);
        resume.setContent(content);
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(resume);
        saveVersion(resume.getId(), content, nextVersionNo(resume.getId()));
        return resume;
    }

    public void delete(Long userId, Long id) {
        getOwned(userId, id);
        resumeMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Resume applyOptimizedContent(Resume resume, String content) {
        resume.setContent(content);
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(resume);
        saveVersion(resume.getId(), content, nextVersionNo(resume.getId()));
        return resume;
    }

    private void saveVersion(Long resumeId, String content, int versionNo) {
        ResumeVersion version = new ResumeVersion();
        version.setResumeId(resumeId);
        version.setVersionNo(versionNo);
        version.setContent(content);
        version.setCreatedAt(LocalDateTime.now());
        resumeVersionMapper.insert(version);
    }

    private int nextVersionNo(Long resumeId) {
        ResumeVersion latest = resumeVersionMapper.selectOne(new LambdaQueryWrapper<ResumeVersion>()
                .eq(ResumeVersion::getResumeId, resumeId)
                .orderByDesc(ResumeVersion::getVersionNo)
                .last("LIMIT 1"));
        return latest == null ? 1 : latest.getVersionNo() + 1;
    }
}
