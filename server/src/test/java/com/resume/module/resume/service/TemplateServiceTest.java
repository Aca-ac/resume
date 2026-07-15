package com.resume.module.resume.service;

import com.resume.common.BusinessException;
import com.resume.module.resume.dto.TemplateRecommendVO;
import com.resume.module.resume.dto.TemplateSaveRequest;
import com.resume.module.resume.dto.TemplateVO;
import com.resume.module.resume.entity.ResumeTemplate;
import com.resume.module.resume.mapper.TemplateMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private TemplateMapper templateMapper;

    @InjectMocks
    private TemplateService templateService;

    @Test
    void recommend_prefersCreativeForDesignRole() {
        ResumeTemplate simple = template(1L, "简约风格", "简约", "应届生、初级岗位");
        ResumeTemplate creative = template(3L, "创意风格", "创意", "设计、创意类岗位");
        when(templateMapper.selectList(any())).thenReturn(List.of(simple, creative));

        List<TemplateRecommendVO> list = templateService.recommend("本科", "互联网", "2", "UI设计");
        assertFalse(list.isEmpty());
        assertEquals(3L, list.get(0).getTemplate().getId());
        assertTrue(list.get(0).getReason().contains("设计") || list.get(0).getScore() > list.get(1).getScore());
    }

    @Test
    void recommend_prefersSimpleForFreshGraduate() {
        ResumeTemplate simple = template(1L, "简约风格", "简约", "应届生、初级岗位");
        ResumeTemplate academic = template(4L, "学术风格", "学术", "学术研究、教育类岗位");
        when(templateMapper.selectList(any())).thenReturn(List.of(simple, academic));

        List<TemplateRecommendVO> list = templateService.recommend("应届", "互联网", "0", "Java开发");
        assertEquals(1L, list.get(0).getTemplate().getId());
    }

    @Test
    void parseYears_usesFirstNumberInRange() {
        assertEquals(3, TemplateService.parseYears("3-5年"));
        assertEquals(0, TemplateService.parseYears("应届"));
        assertEquals(10, TemplateService.parseYears("10年"));
        assertEquals(0, TemplateService.parseYears(null));
    }

    @Test
    void normalizeTemplatePath_rejectsOutsideResumesFolder() {
        assertEquals("/templates/resumes/simple/template.docx",
                TemplateService.normalizeTemplatePath("/templates/resumes/simple/template.docx"));
        assertThrows(BusinessException.class,
                () -> TemplateService.normalizeTemplatePath("/templates/previews/x.docx"));
        assertThrows(BusinessException.class,
                () -> TemplateService.normalizeTemplatePath("../secret.docx"));
        assertThrows(BusinessException.class,
                () -> TemplateService.normalizeTemplatePath(""));
    }

    @Test
    void create_persistsNormalizedPath() {
        TemplateSaveRequest req = new TemplateSaveRequest();
        req.setName("简约");
        req.setCategory("简约");
        req.setTemplatePath("templates/resumes/simple/template.docx");
        when(templateMapper.insert(any(ResumeTemplate.class))).thenAnswer(inv -> {
            ResumeTemplate t = inv.getArgument(0);
            t.setId(99L);
            return 1;
        });

        TemplateVO vo = templateService.create(req);
        assertEquals(99L, vo.getId());

        ArgumentCaptor<ResumeTemplate> cap = ArgumentCaptor.forClass(ResumeTemplate.class);
        verify(templateMapper).insert(cap.capture());
        assertEquals("/templates/resumes/simple/template.docx", cap.getValue().getTemplatePath());
    }

    @Test
    void require_missing_throws404() {
        when(templateMapper.selectById(404L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> templateService.get(404L));
        assertEquals(404, ex.getCode());
    }

    private static ResumeTemplate template(Long id, String name, String category, String scene) {
        ResumeTemplate t = new ResumeTemplate();
        t.setId(id);
        t.setName(name);
        t.setCategory(category);
        t.setApplicableScene(scene);
        t.setTemplatePath("/templates/resumes/x/template.docx");
        return t;
    }
}
