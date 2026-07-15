package com.resume.module.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.common.BusinessException;
import com.resume.common.TextSanitizer;
import com.resume.config.StorageProperties;
import com.resume.module.resume.dto.ChunkUploadVO;
import com.resume.module.resume.dto.ImportResultVO;
import com.resume.module.resume.dto.ResumeSaveRequest;
import com.resume.module.resume.dto.ResumeVO;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeDetail;
import com.resume.module.resume.entity.ResumeFile;
import com.resume.module.resume.entity.ResumeSemanticVector;
import com.resume.module.resume.mapper.ResumeDetailMapper;
import com.resume.module.resume.mapper.ResumeFileMapper;
import com.resume.module.resume.mapper.ResumeMapper;
import com.resume.module.resume.mapper.ResumeSemanticVectorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/** Resume domain service — body read/write goes through ResumeSummaryStore. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_IMPORT = "IMPORT";

    private final ResumeMapper resumeMapper;
    private final ResumeDetailMapper resumeDetailMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final ResumeSemanticVectorMapper resumeSemanticVectorMapper;
    private final FileStorageService fileStorageService;
    private final FileParseService fileParseService;
    private final ResumeExportService exportService;
    private final OcrService ocrService;
    private final ChunkUploadService chunkUploadService;
    private final StorageProperties storageProperties;
    private final ResumeOptimizeService optimizeService;
    private final ResumePhotoService resumePhotoService;
    private final ResumeSummaryStore summaryStore;

    // ========== 简历 CRUD（正文存 resume_details.SUMMARY）==========

    @Transactional
    public ResumeVO createResume(Long userId, String title) {
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(defaultTitle(title));
        resume.setSourceType(SOURCE_MANUAL);
        resume.setVersion(1);
        resumeMapper.insert(resume);
        summaryStore.save(resume.getId(), "");
        return toVO(resume, "");
    }

    @Transactional
    public ResumeVO createResume(Long userId, ResumeSaveRequest req) {
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(defaultTitle(req.getTitle()));
        resume.setSourceType(SOURCE_MANUAL);
        resume.setVersion(1);
        resumeMapper.insert(resume);
        if (req.getJobIntention() != null) {
            resume.setJobIntention(req.getJobIntention().trim());
            resumeMapper.updateById(resume);
        }
        String content = defaultContent(req.getContent());
        summaryStore.save(resume.getId(), content);
        return toVO(resume, summaryStore.load(resume.getId()));
    }

    public ResumeVO getResumeVo(Long id, Long userId) {
        Resume resume = getResume(id, userId);
        return toVO(resume, summaryStore.load(id));
    }

    public Resume getResume(Long id, Long userId) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BusinessException(404, "简历不存在");
        }
        return resume;
    }

    public List<ResumeVO> listResumes(Long userId) {
        List<Resume> rows = resumeMapper.selectList(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .orderByDesc(Resume::getUpdatedAt));
        return rows.stream()
                .map(r -> toVO(r, previewSummary(r.getId())))
                .toList();
    }

    public void updateResumeTitle(Long id, Long userId, String title) {
        Resume resume = getResume(id, userId);
        resume.setTitle(title);
        resumeMapper.updateById(resume);
    }

    /**
     * 更新简历标题 / 正文。正文写入 resume_details.SUMMARY，是编辑页「保存修改」的唯一更新入口。
     */
    @Transactional
    @Transactional
    public ResumeVO updateResume(Long id, Long userId, ResumeSaveRequest req) {
        // 远端main新增：请求体非空校验
        if (req == null) {
            throw new BusinessException(400, "请求体不能为空");
        }
        Resume resume = getResume(id, userId);
        // 本地HEAD原有：读取更新前原始正文，用于对比是否变更
        String originalContent = summaryStore.load(id);
        boolean touched = false;

        // 标题更新+空值兜底（远端main逻辑）
        if (req.getTitle() != null) {
            resume.setTitle(req.getTitle().isBlank() ? "未命名简历" : req.getTitle().trim());
            touched = true;
        }

        // 正文更新（远端main逻辑）
        if (req.getContent() != null) {
            summaryStore.save(id, req.getContent());
            touched = true;
        }

        // 求职意向更新（远端main新增）
        if (req.getJobIntention() != null) {
            resume.setJobIntention(req.getJobIntention().trim());
            touched = true;
        }

        // 无任何字段修改直接抛异常（远端main新增）
        if (!touched) {
            throw new BusinessException(400, "请提供 title、content 或 jobIntention 以更新简历");
        }

        // 版本号自增（远端main新增）
        resume.setVersion(resume.getVersion() == null ? 1 : resume.getVersion() + 1);
        resumeMapper.updateById(resume);

        // 本地HEAD核心逻辑：正文发生变化则标记向量失效
        if (req.getContent() != null && !req.getContent().equals(originalContent)) {
            invalidateResumeVector(id);
        }

        // 远端main新增：打印更新日志
        String saved = summaryStore.load(id);
        log.info("Resume updated id={}, version={}, contentChars={}", id, resume.getVersion(), saved.length());
        return toVO(resume, saved);
    }
}