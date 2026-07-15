package com.resume.module.resume.service;

import com.resume.common.BusinessException;
import com.resume.module.resume.dto.ResumeSaveRequest;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeDetail;
import com.resume.module.resume.mapper.ResumeDetailMapper;
import com.resume.module.resume.mapper.ResumeFileMapper;
import com.resume.module.resume.mapper.ResumeMapper;
import com.resume.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock private ResumeMapper resumeMapper;
    @Mock private ResumeDetailMapper resumeDetailMapper;
    @Mock private ResumeFileMapper resumeFileMapper;
    @Mock private FileStorageService fileStorageService;
    @Mock private FileParseService fileParseService;
    @Mock private ResumeExportService exportService;
    @Mock private OcrService ocrService;
    @Mock private ChunkUploadService chunkUploadService;
    @Mock private StorageProperties storageProperties;
    @Mock private ResumeOptimizeService optimizeService;
    @Mock private ResumePhotoService resumePhotoService;
    @Mock private ResumeSummaryStore summaryStore;

    @InjectMocks
    private ResumeService resumeService;

    @Test
    void getResume_missingOrWrongOwner_throws404() {
        when(resumeMapper.selectById(1L)).thenReturn(null);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> resumeService.getResume(1L, 1L)).getCode());

        Resume other = new Resume();
        other.setId(2L);
        other.setUserId(9L);
        when(resumeMapper.selectById(2L)).thenReturn(other);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> resumeService.getResume(2L, 1L)).getCode());
    }

    @Test
    void updateResume_nullBody_throws400() {
        assertEquals(400, assertThrows(BusinessException.class,
                () -> resumeService.updateResume(1L, 1L, null)).getCode());
    }

    @Test
    void updateResume_neitherTitleNorContent_throws400() {
        Resume resume = new Resume();
        resume.setId(1L);
        resume.setUserId(1L);
        resume.setVersion(1);
        when(resumeMapper.selectById(1L)).thenReturn(resume);

        ResumeSaveRequest req = new ResumeSaveRequest();
        assertEquals(400, assertThrows(BusinessException.class,
                () -> resumeService.updateResume(1L, 1L, req)).getCode());
    }

    @Test
    void updateResume_savesContentViaSummaryStore() {
        Resume resume = new Resume();
        resume.setId(1L);
        resume.setUserId(1L);
        resume.setTitle("t");
        resume.setVersion(1);
        when(resumeMapper.selectById(1L)).thenReturn(resume);
        when(summaryStore.load(1L)).thenReturn("saved");

        ResumeSaveRequest req = new ResumeSaveRequest();
        req.setContent("body");
        var vo = resumeService.updateResume(1L, 1L, req);

        verify(summaryStore).save(1L, "body");
        verify(resumeMapper).updateById(resume);
        assertEquals(2, resume.getVersion());
        assertEquals("saved", vo.getContent());
    }

    @Test
    void updateDetail_missing_throws404() {
        when(resumeDetailMapper.selectById(8L)).thenReturn(null);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> resumeService.updateDetail(8L, 1L, "x")).getCode());
    }

    @Test
    void updateDetail_wrongOwner_throws403() {
        ResumeDetail detail = new ResumeDetail();
        detail.setId(8L);
        detail.setResumeId(3L);
        when(resumeDetailMapper.selectById(8L)).thenReturn(detail);
        Resume resume = new Resume();
        resume.setUserId(99L);
        when(resumeMapper.selectById(3L)).thenReturn(resume);

        assertEquals(403, assertThrows(BusinessException.class,
                () -> resumeService.updateDetail(8L, 1L, "x")).getCode());
    }
}
