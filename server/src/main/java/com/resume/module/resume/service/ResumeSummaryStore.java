package com.resume.module.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.resume.common.TextSanitizer;
import com.resume.module.resume.entity.ResumeDetail;
import com.resume.module.resume.mapper.ResumeDetailMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Single source of truth for resume_details.SUMMARY load/save.
 * Public methods are transactional so callers outside a larger txn still stay atomic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeSummaryStore {

    public static final String SECTION_SUMMARY = "SUMMARY";

    private final ResumeDetailMapper resumeDetailMapper;

    public String load(Long resumeId) {
        ResumeDetail detail = findCanonical(resumeId);
        return detail == null || detail.getContent() == null ? "" : detail.getContent();
    }

    /**
     * Upsert the canonical SUMMARY row and delete any other SUMMARY rows for this resume.
     * Final delete re-queries so concurrent inserts mid-flight are also cleaned up.
     */
    @Transactional
    public void save(Long resumeId, String content) {
        String cleaned = TextSanitizer.forStoredContent(content);
        List<ResumeDetail> summaries = listSummaryRows(resumeId);
        if (summaries.isEmpty()) {
            ResumeDetail created = insertSummary(resumeId, cleaned);
            deleteOtherSummaries(resumeId, created.getId());
            return;
        }

        ResumeDetail canonical = pickCanonical(summaries);
        String sectionName = canonical.getSectionName() == null || canonical.getSectionName().isBlank()
                ? "正文" : canonical.getSectionName();
        int rows = resumeDetailMapper.update(null, new LambdaUpdateWrapper<ResumeDetail>()
                .eq(ResumeDetail::getId, canonical.getId())
                .set(ResumeDetail::getContent, cleaned)
                .set(ResumeDetail::getSectionName, sectionName)
                .set(ResumeDetail::getSectionType, SECTION_SUMMARY)
                .set(ResumeDetail::getUpdatedAt, LocalDateTime.now()));
        if (rows == 0) {
            canonical = insertSummary(resumeId, cleaned);
        }
        deleteOtherSummaries(resumeId, canonical.getId());
        log.debug("SUMMARY saved resumeId={}, detailId={}, chars={}", resumeId, canonical.getId(), cleaned.length());
    }

    public ResumeDetail findCanonical(Long resumeId) {
        List<ResumeDetail> summaries = listSummaryRows(resumeId);
        if (summaries.isEmpty()) {
            return null;
        }
        return pickCanonical(summaries);
    }

    private ResumeDetail insertSummary(Long resumeId, String cleaned) {
        ResumeDetail detail = new ResumeDetail();
        detail.setResumeId(resumeId);
        detail.setSectionType(SECTION_SUMMARY);
        detail.setSectionName("正文");
        detail.setContent(cleaned);
        detail.setSortOrder(0);
        resumeDetailMapper.insert(detail);
        return detail;
    }

    /** Delete every SUMMARY row for this resume except keepId (re-query covers race inserts). */
    private void deleteOtherSummaries(Long resumeId, Long keepId) {
        if (keepId == null) {
            return;
        }
        resumeDetailMapper.delete(new LambdaQueryWrapper<ResumeDetail>()
                .eq(ResumeDetail::getResumeId, resumeId)
                .eq(ResumeDetail::getSectionType, SECTION_SUMMARY)
                .ne(ResumeDetail::getId, keepId));
    }

    private List<ResumeDetail> listSummaryRows(Long resumeId) {
        return resumeDetailMapper.selectList(new LambdaQueryWrapper<ResumeDetail>()
                .eq(ResumeDetail::getResumeId, resumeId)
                .eq(ResumeDetail::getSectionType, SECTION_SUMMARY));
    }

    /**
     * Prefer latest updated_at, then largest id — never content length
     * (shortening body must not be overridden by an older longer duplicate).
     */
    static ResumeDetail pickCanonical(List<ResumeDetail> summaries) {
        return summaries.stream()
                .max(Comparator
                        .comparing(ResumeDetail::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(ResumeDetail::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(summaries.get(0));
    }
}
