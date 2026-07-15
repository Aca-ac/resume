package com.resume.module.resume.service;

import com.resume.common.BusinessException;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.mapper.MatchAnalysisMapper;
import com.resume.module.resume.mapper.MatchDimensionMapper;
import com.resume.module.resume.mapper.MatchRecordMapper;
import com.resume.module.resume.mapper.MatchSubDimensionMapper;
import com.resume.module.resume.mapper.ResumeDetailMapper;
import com.resume.module.resume.mapper.ResumeMapper;
import com.resume.module.job.mapper.TargetJobMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock private MatchRecordMapper matchRecordMapper;
    @Mock private MatchAnalysisMapper matchAnalysisMapper;
    @Mock private MatchDimensionMapper matchDimensionMapper;
    @Mock private MatchSubDimensionMapper matchSubDimensionMapper;
    @Mock private ResumeMapper resumeMapper;
    @Mock private ResumeDetailMapper resumeDetailMapper;
    @Mock private TargetJobMapper targetJobMapper;
    @Mock private ObjectMapper objectMapper;
    @Mock private ResourceLoader resourceLoader;

    @InjectMocks
    private MatchService matchService;

    @Test
    void matchJd_nullResumeId_throws400() {
        assertEquals(400, assertThrows(BusinessException.class,
                () -> matchService.matchJd(1L, null, "jd")).getCode());
    }

    @Test
    void matchJd_blankJd_throws400() {
        assertEquals(400, assertThrows(BusinessException.class,
                () -> matchService.matchJd(1L, 2L, "  ")).getCode());
    }

    @Test
    void matchJd_missingResume_throws404() {
        when(resumeMapper.selectById(2L)).thenReturn(null);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> matchService.matchJd(1L, 2L, "Java开发")).getCode());
    }

    @Test
    void matchJd_emptyResumeContent_throws400() {
        Resume resume = new Resume();
        resume.setId(2L);
        resume.setUserId(1L);
        when(resumeMapper.selectById(2L)).thenReturn(resume);
        when(resumeDetailMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertEquals(400, assertThrows(BusinessException.class,
                () -> matchService.matchJd(1L, 2L, "Java开发")).getCode());
    }

    @Test
    void matchByJobId_nullJobId_throws400() {
        assertEquals(400, assertThrows(BusinessException.class,
                () -> matchService.matchByJobId(1L, 2L, null)).getCode());
    }

    @Test
    void getAnalysisById_missing_throws404() {
        when(matchAnalysisMapper.selectById(9L)).thenReturn(null);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> matchService.getAnalysisById(1L, 9L)).getCode());
    }
}
