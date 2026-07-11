package com.resume.module.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.BusinessException;
import com.resume.module.resume.dto.MatchRecordVO;
import com.resume.module.resume.dto.PageResult;
import com.resume.module.resume.entity.MatchRecord;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeDetail;
import com.resume.module.resume.mapper.MatchRecordMapper;
import com.resume.module.resume.mapper.ResumeDetailMapper;
import com.resume.module.resume.mapper.ResumeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MatchRecordMapper matchRecordMapper;
    private final ResumeMapper resumeMapper;
    private final ResumeDetailMapper resumeDetailMapper;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();
    private final ResourceLoader resourceLoader;

    @Value("${app.ai.qwen.api-key:}")
    private String apiKey;

    @Value("${app.ai.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${app.ai.qwen.model:qwen-plus}")
    private String model;

    public MatchRecordVO matchJd(Long userId, Long resumeId, String jdText) {
        if (resumeId == null) {
            throw new BusinessException(400, "请选择简历");
        }
        if (jdText == null || jdText.isBlank()) {
            throw new BusinessException(400, "请粘贴职位描述");
        }
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BusinessException(404, "简历不存在");
        }
        String resumeContent = buildResumeContent(resumeId);
        if (resumeContent.isBlank()) {
            throw new BusinessException(400, "简历内容为空，请先完善简历");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(500, "未配置 DASHSCOPE_API_KEY，无法进行匹配分析");
        }

        String aiRaw = callAi(resumeContent, jdText.trim());
        ParsedMatch parsed = parseAiResult(aiRaw);

        MatchRecord record = new MatchRecord();
        record.setUserId(userId);
        record.setResumeId(resumeId);
        record.setJdText(jdText.trim());
        record.setMatchScore(parsed.score());
        record.setSummaryScore(parsed.summaryScore());
        record.setEducationScore(parsed.educationScore());
        record.setExperienceScore(parsed.experienceScore());
        record.setSkillScore(parsed.skillScore());
        record.setProjectScore(parsed.projectScore());
        record.setAnalysis(parsed.analysis());
        record.setCreatedAt(LocalDateTime.now());
        matchRecordMapper.insert(record);
        return toVo(record);
    }

    public PageResult<MatchRecordVO> listHistory(Long userId, int page, int size) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1 || size > 100) {
            size = 50;
        }
        Page<MatchRecord> result = matchRecordMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<MatchRecord>()
                        .eq(MatchRecord::getUserId, userId)
                        .orderByDesc(MatchRecord::getCreatedAt)
        );
        List<MatchRecordVO> records = result.getRecords().stream().map(this::toVo).toList();
        return new PageResult<>(records, result.getTotal(), page, size);
    }

    private String callAi(String resumeContent, String jdText) {
        String promptTemplate = loadPromptTemplate();
        String prompt = promptTemplate.replace("【在此处粘贴简历全文】", resumeContent)
                + "\n\n## 六、职位描述(JD)信息\n【职位描述】\n" + jdText;

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .headers(h -> h.addAll(headers))
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String content = parseChatContent(resp);
            if (content == null || content.isBlank()) {
                throw new BusinessException(500, "AI 未返回有效内容");
            }
            return content.trim();
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("Match API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(500, "匹配分析服务异常：" + e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("Match failed: {}", e.getMessage());
            throw new BusinessException(500, "匹配分析失败：" + e.getMessage());
        }
    }

    private String loadPromptTemplate() {
        try {
            Resource resource = resourceLoader.getResource("classpath:prompts/resume_analysis_prompt.txt");
            if (!resource.exists()) {
                throw new BusinessException(500, "提示词文件不存在");
            }
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to load prompt template: {}", e.getMessage());
            throw new BusinessException(500, "加载提示词文件失败");
        }
    }

    private String parseChatContent(Map<String, Object> resp) {
        if (resp == null) {
            return null;
        }
        Object choices = resp.get("choices");
        if (!(choices instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> choice)) {
            return null;
        }
        Object message = choice.get("message");
        if (!(message instanceof Map<?, ?> msg)) {
            return null;
        }
        Object content = msg.get("content");
        return content == null ? null : content.toString();
    }

    private ParsedMatch parseAiResult(String raw) {
        String json = extractJsonObject(raw);
        try {
            JsonNode node = objectMapper.readTree(json);
            int summaryScore = clampScore(node.path("summary_score").asInt(-1));
            int educationScore = clampScore(node.path("education_score").asInt(-1));
            int experienceScore = clampScore(node.path("experience_score").asInt(-1));
            int skillScore = clampScore(node.path("skill_score").asInt(-1));
            int projectScore = clampScore(node.path("project_score").asInt(-1));
            String analysis = node.path("suggestions").asText("");

            int totalScore = calculateTotalScore(summaryScore, educationScore, experienceScore, skillScore, projectScore);

            if (analysis.isBlank()) {
                analysis = raw;
            }
            return new ParsedMatch(totalScore, summaryScore, educationScore, experienceScore, skillScore, projectScore, analysis.trim());
        } catch (Exception e) {
            log.warn("Failed to parse AI result: {}", e.getMessage());
            return new ParsedMatch(60, 0, 0, 0, 0, 0, raw);
        }
    }

    private int calculateTotalScore(int summary, int education, int experience, int skill, int project) {
        double total = summary * 0.10 + education * 0.15 + experience * 0.25 + skill * 0.25 + project * 0.25;
        return (int) Math.round(total);
    }

    private String extractJsonObject(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    private int clampScore(int score) {
        if (score < 0) {
            return 0;
        }
        return Math.min(score, 100);
    }

    private String buildResumeContent(Long resumeId) {
        List<ResumeDetail> details = resumeDetailMapper.selectList(
                new LambdaQueryWrapper<ResumeDetail>()
                        .eq(ResumeDetail::getResumeId, resumeId)
                        .orderByAsc(ResumeDetail::getSortOrder)
        );
        if (details.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (ResumeDetail detail : details) {
            builder.append("【").append(detail.getSectionName()).append("】\n");
            builder.append(detail.getContent()).append("\n\n");
        }
        return builder.toString();
    }

    private MatchRecordVO toVo(MatchRecord record) {
        MatchRecordVO vo = new MatchRecordVO();
        vo.setId(record.getId());
        vo.setResumeId(record.getResumeId());
        vo.setMatchScore(record.getMatchScore());
        vo.setSummaryScore(record.getSummaryScore());
        vo.setEducationScore(record.getEducationScore());
        vo.setExperienceScore(record.getExperienceScore());
        vo.setSkillScore(record.getSkillScore());
        vo.setProjectScore(record.getProjectScore());
        vo.setAnalysis(record.getAnalysis());
        vo.setCreatedAt(record.getCreatedAt() == null ? null : record.getCreatedAt().format(FMT));
        return vo;
    }

    private record ParsedMatch(int score, int summaryScore, int educationScore, int experienceScore, int skillScore, int projectScore, String analysis) {
    }
}
