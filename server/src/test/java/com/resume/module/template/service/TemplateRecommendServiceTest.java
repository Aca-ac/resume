package com.resume.module.template.service;

import com.resume.module.template.dto.TemplateRecommendRequest;
import com.resume.module.template.dto.TemplateRecommendVO;
import com.resume.module.template.entity.Template;
import com.resume.module.template.mapper.TemplateMapper;
import com.resume.module.template.support.TemplatePlaceholders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateRecommendServiceTest {

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private TemplateRecommendService recommendService;

    private Template tech;
    private Template campus;

    @BeforeEach
    void setUp() {
        tech = new Template();
        tech.setId(1L);
        tech.setName("技术岗简洁");
        tech.setCategory("技术岗");
        tech.setApplicableScene("Java后端开发");
        tech.setTemplatePath("docx/a.docx");

        campus = new Template();
        campus.setId(2L);
        campus.setName("校园招聘");
        campus.setCategory("应届生");
        campus.setApplicableScene("校招简洁模板");
        campus.setTemplatePath("docx/b.docx");
    }

    @Test
    void recommend_prefersTechForJavaDev() {
        when(templateService.listAll()).thenReturn(List.of(tech, campus));
        when(templateService.toVO(tech)).thenAnswer(inv -> {
            var t = (Template) inv.getArgument(0);
            var vo = new com.resume.module.template.dto.TemplateVO();
            vo.setId(t.getId());
            vo.setName(t.getName());
            vo.setCategory(t.getCategory());
            return vo;
        });
        when(templateService.toVO(campus)).thenAnswer(inv -> {
            var t = (Template) inv.getArgument(0);
            var vo = new com.resume.module.template.dto.TemplateVO();
            vo.setId(t.getId());
            vo.setName(t.getName());
            vo.setCategory(t.getCategory());
            return vo;
        });

        TemplateRecommendRequest req = new TemplateRecommendRequest();
        req.setTargetPosition("Java开发");
        req.setIndustry("互联网");
        req.setWorkYears("3");
        req.setEducation("本科");

        List<TemplateRecommendVO> list = recommendService.recommend(req);
        assertFalse(list.isEmpty());
        assertEquals(1L, list.get(0).getTemplate().getId());
        assertTrue(list.get(0).getReason().contains("技术"));
    }

    @Test
    void placeholder_mapsSectionTypes() {
        assertEquals(TemplatePlaceholders.EDUCATION, TemplatePlaceholders.mapSectionType("EDUCATION"));
        assertEquals(TemplatePlaceholders.EXPERIENCE, TemplatePlaceholders.mapSectionType("WORK_EXPERIENCE"));
        assertEquals(TemplatePlaceholders.SKILLS, TemplatePlaceholders.mapSectionType("SKILL"));
    }
}
