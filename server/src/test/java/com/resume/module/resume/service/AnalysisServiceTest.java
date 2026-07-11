package com.resume.module.resume.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.BusinessException;
import com.resume.module.analyze.util.AnalyzeUtil;
import com.resume.module.resume.dto.AnalysisResult;
import com.resume.module.resume.entity.AnalysisRecord;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.mapper.AnalysisRecordMapper;
import com.resume.module.resume.mapper.ResumeDetailMapper;
import com.resume.module.resume.mapper.ResumeMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AnalysisRecordMapper analysisRecordMapper = mock(AnalysisRecordMapper.class);
    private final ResumeMapper resumeMapper = mock(ResumeMapper.class);
    private final ResumeDetailMapper resumeDetailMapper = mock(ResumeDetailMapper.class);
    private final AnalyzeUtil analyzeUtil = mock(AnalyzeUtil.class);
    private final AnalysisService analysisService = new AnalysisService(
            analysisRecordMapper,
            resumeMapper,
            resumeDetailMapper,
            objectMapper,
            analyzeUtil
    );

    @Test
    void parseAiJson_shouldParseNestedScoresJson() throws Exception {
        JsonNode node = analysisService.parseAiJson("""
                {"scores":{"summary":80,"education":75,"experience":85,"skill":90,"project":88},"suggestions":"建议优化项目描述"}
                """);
        assertEquals(80, node.get("scores").get("summary").asInt());
        assertEquals("建议优化项目描述", node.get("suggestions").asText());
    }

    @Test
    void parseAiJson_shouldParseFlatScoreJson() throws Exception {
        JsonNode node = analysisService.parseAiJson("""
                {"summary_score":80,"education_score":75,"experience_score":85,"skill_score":90,"project_score":88,"suggestions":"ok"}
                """);
        assertEquals(80, node.get("summary_score").asInt());
        assertEquals("ok", node.get("suggestions").asText());
    }

    @Test
    void parseAiJson_shouldParseMarkdownWrappedJson() throws Exception {
        JsonNode node = analysisService.parseAiJson("""
                分析结果如下：
                ```json
                {"scores":{"summary":70,"education":72,"experience":73,"skill":74,"project":75},"suggestions":"ok"}
                ```
                """);
        assertEquals(74, node.get("scores").get("skill").asInt());
        assertTrue(node.has("suggestions"));
    }

    @Test
    void parseAiJson_shouldRejectMissingScores() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> analysisService.parseAiJson("{\"suggestions\":\"ok\"}")
        );

        assertEquals("AI 返回 JSON 缺少评分字段", exception.getMessage());
    }

    @Test
    void listRecords_shouldReturnPagedUserHistory() {
        AnalysisRecord record = completedRecord(10L, 1L, 3L);
        Resume resume = new Resume();
        resume.setId(3L);
        resume.setTitle("Java 后端简历");

        when(analysisRecordMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<AnalysisRecord> page = invocation.getArgument(0);
            page.setRecords(List.of(record));
            page.setTotal(1);
            return page;
        });
        when(resumeMapper.selectById(3L)).thenReturn(resume);

        Map<String, Object> payload = analysisService.listRecords(1L, 1, 10);

        assertEquals(1L, payload.get("total"));
        assertEquals(1L, payload.get("page"));
        assertEquals(10L, payload.get("size"));
        List<?> records = (List<?>) payload.get("records");
        AnalysisResult result = (AnalysisResult) records.get(0);
        assertEquals(10L, result.getId());
        assertEquals("Java 后端简历", result.getResumeTitle());
        assertEquals("completed", result.getStatus());
        assertEquals(82, result.getTotalScore());
    }

    @Test
    void listRecords_shouldRejectInvalidPageParams() {
        assertThrows(BusinessException.class, () -> analysisService.listRecords(1L, 0, 10));
        assertThrows(BusinessException.class, () -> analysisService.listRecords(1L, 1, 101));
    }

    @Test
    void getRecord_shouldReturnOwnedRecordDetail() {
        AnalysisRecord record = completedRecord(10L, 1L, 3L);
        Resume resume = new Resume();
        resume.setId(3L);
        resume.setTitle("产品经理简历");

        when(analysisRecordMapper.selectById(10L)).thenReturn(record);
        when(resumeMapper.selectById(3L)).thenReturn(resume);

        AnalysisResult result = analysisService.getRecord(10L, 1L);

        assertEquals(10L, result.getId());
        assertEquals(3L, result.getResumeId());
        assertEquals("产品经理简历", result.getResumeTitle());
        assertEquals(90, result.getScores().get("skill"));
    }

    @Test
    void getRecord_shouldRejectOtherUsersRecord() {
        when(analysisRecordMapper.selectById(10L)).thenReturn(completedRecord(10L, 2L, 3L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> analysisService.getRecord(10L, 1L)
        );

        assertEquals(404, exception.getCode());
        assertEquals("分析记录不存在", exception.getMessage());
    }

    @Test
    void analyzeResume_shouldMarkRecordFailedWhenMaasKeyMissing() {
        Resume resume = new Resume();
        resume.setId(3L);
        resume.setUserId(1L);
        resume.setTitle("测试简历");

        when(resumeMapper.selectById(3L)).thenReturn(resume);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> analysisService.analyzeResume(1L, 3L)
        );

        assertEquals(500, exception.getCode());
        assertEquals("AI 分析服务未配置", exception.getMessage());
        verify(analysisRecordMapper).insert(any(AnalysisRecord.class));
        verify(analysisRecordMapper).updateById(argThat(record ->
                record.getStatus().equals(2)
                        && "AI 分析服务未配置".equals(record.getErrorMessage())
        ));
    }

    private AnalysisRecord completedRecord(Long id, Long userId, Long resumeId) {
        AnalysisRecord record = new AnalysisRecord();
        record.setId(id);
        record.setUserId(userId);
        record.setResumeId(resumeId);
        record.setSummaryScore(80);
        record.setEducationScore(75);
        record.setExperienceScore(85);
        record.setSkillScore(90);
        record.setProjectScore(78);
        record.setTotalScore(82);
        record.setSuggestions("建议强化项目成果");
        record.setStatus(1);
        record.setCreatedAt(LocalDateTime.of(2026, 7, 9, 10, 30));
        return record;
    }
}
