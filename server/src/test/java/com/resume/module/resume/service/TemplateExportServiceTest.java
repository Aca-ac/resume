package com.resume.module.resume.service;

import com.resume.common.BusinessException;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateExportServiceTest {

    @Mock private ResumeService resumeService;
    @Mock private TemplateService templateService;
    @Mock private com.resume.module.resume.mapper.ResumeDetailMapper resumeDetailMapper;
    @Mock private com.resume.user_identify.mapper.UserMapper userMapper;
    @Mock private TemplateRenderService templateRenderService;
    @Mock private LibreOfficePdfConverter pdfConverter;
    @Mock private ExportStorageService exportStorageService;
    @Mock private ResumePhotoService resumePhotoService;
    @Mock private ResumeSummaryStore summaryStore;

    @InjectMocks
    private TemplateExportService templateExportService;

    @Test
    void createExportJob_blankFormat_throws400() {
        assertEquals(400, assertThrows(BusinessException.class,
                () -> templateExportService.createExportJob(1L, 2L, 3L, " ")).getCode());
    }

    @Test
    void createExportJob_unknownFormat_throws400() throws Exception {
        Resume resume = new Resume();
        resume.setId(2L);
        resume.setTitle("r");
        when(resumeService.getResume(2L, 1L)).thenReturn(resume);
        ResumeTemplate tpl = new ResumeTemplate();
        tpl.setId(3L);
        tpl.setName("t");
        when(templateService.require(3L)).thenReturn(tpl);

        assertEquals(400, assertThrows(BusinessException.class,
                () -> templateExportService.createExportJob(1L, 2L, 3L, "xlsx")).getCode());
    }

    @Test
    void resolveTemplatePath_blank_throws500() {
        ResumeTemplate tpl = new ResumeTemplate();
        tpl.setId(3L);
        tpl.setTemplatePath("  ");
        assertEquals(500, assertThrows(BusinessException.class,
                () -> templateExportService.resolveTemplatePath(tpl)).getCode());
    }

    @Test
    void resolveTemplatePath_ok() {
        ResumeTemplate tpl = new ResumeTemplate();
        tpl.setTemplatePath("/templates/resumes/simple/template.docx");
        assertEquals("/templates/resumes/simple/template.docx",
                templateExportService.resolveTemplatePath(tpl));
    }
}
