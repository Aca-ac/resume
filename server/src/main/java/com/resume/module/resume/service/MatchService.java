package com.resume.module.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.BusinessException;
import com.resume.module.resume.config.MatchDimensionConfig;
import com.resume.module.resume.config.MatchDimensionConfig.DimensionDef;
import com.resume.module.resume.config.MatchDimensionConfig.Preset;
import com.resume.module.resume.dto.*;
import com.resume.module.resume.entity.MatchAnalysis;
import com.resume.module.resume.entity.MatchDimension;
import com.resume.module.resume.entity.MatchRecord;
import com.resume.module.resume.entity.MatchSubDimension;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeDetail;
import com.resume.module.resume.mapper.MatchAnalysisMapper;
import com.resume.module.resume.mapper.MatchDimensionMapper;
import com.resume.module.resume.mapper.MatchRecordMapper;
import com.resume.module.resume.mapper.MatchSubDimensionMapper;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<SuggestionVO>> SUGGESTION_LIST_TYPE = new TypeReference<>() {
    };

    private final MatchRecordMapper matchRecordMapper;
    private final MatchAnalysisMapper matchAnalysisMapper;
    private final MatchDimensionMapper matchDimensionMapper;
    private final MatchSubDimensionMapper matchSubDimensionMapper;
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

    @Value("${app.match.dimension-preset:general}")
    private String dimensionPreset;

    @Transactional
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
        Preset preset = resolvePreset(dimensionPreset);
        List<BuiltDimension> dimensions = buildDimensions(parsed, preset);
        int radarScore = calculateRadarScore(dimensions);
        String matchLevel = resolveMatchLevel(radarScore);
        String summary = buildSummary(parsed);
        List<String> highlights = parsed.highlights().isEmpty()
                ? deriveHighlights(dimensions) : parsed.highlights();
        List<String> weaknesses = parsed.weaknesses().isEmpty()
                ? deriveWeaknesses(dimensions) : parsed.weaknesses();
        List<SuggestionVO> suggestions = parsed.suggestions().isEmpty()
                ? defaultSuggestions(parsed.analysis()) : parsed.suggestions();

        MatchAnalysis analysis = persistAnalysis(
                userId, resumeId, jdText.trim(), radarScore, matchLevel,
                parsed.analysis(), summary, highlights, weaknesses, suggestions, dimensions
        );

        MatchRecord record = new MatchRecord();
        record.setUserId(userId);
        record.setResumeId(resumeId);
        record.setJdText(jdText.trim());
        record.setMatchScore(radarScore);
        record.setSummaryScore(parsed.summaryScore());
        record.setEducationScore(parsed.educationScore());
        record.setExperienceScore(parsed.experienceScore());
        record.setSkillScore(parsed.skillScore());
        record.setProjectScore(parsed.projectScore());
        record.setAnalysisId(analysis.getId());
        record.setAnalysis(parsed.analysis());
        record.setCreatedAt(LocalDateTime.now());
        matchRecordMapper.insert(record);
        return toVo(record);
    }

    public MatchAnalysisResultVO getAnalysisById(Long userId, Long analysisId) {
        MatchAnalysis analysis = matchAnalysisMapper.selectById(analysisId);
        if (analysis == null || !analysis.getUserId().equals(userId)) {
            throw new BusinessException(404, "分析记录不存在");
        }
        return toAnalysisResultVo(analysis);
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

    private MatchAnalysis persistAnalysis(Long userId, Long resumeId, String jdText, int matchScore,
                                          String matchLevel, String analysisText, String summary,
                                          List<String> highlights, List<String> weaknesses,
                                          List<SuggestionVO> suggestions, List<BuiltDimension> dimensions) {
        MatchAnalysis analysis = new MatchAnalysis();
        analysis.setUserId(userId);
        analysis.setResumeId(resumeId);
        analysis.setJdText(jdText);
        analysis.setMatchScore(matchScore);
        analysis.setMatchLevel(matchLevel);
        analysis.setAnalysis(analysisText);
        analysis.setSummary(summary);
        analysis.setHighlights(writeJson(highlights));
        analysis.setWeaknesses(writeJson(weaknesses));
        analysis.setSuggestions(writeJson(suggestions));
        analysis.setCreatedAt(LocalDateTime.now());
        analysis.setUpdatedAt(LocalDateTime.now());
        matchAnalysisMapper.insert(analysis);

        int sortOrder = 0;
        for (BuiltDimension built : dimensions) {
            DimensionDef def = built.definition();
            MatchDimension dimension = new MatchDimension();
            dimension.setAnalysisId(analysis.getId());
            dimension.setDimensionKey(def.getKey());
            dimension.setDimensionName(def.getName());
            dimension.setScore(built.score());
            dimension.setWeight(def.getWeight());
            dimension.setDescription(def.getDescription());
            dimension.setDetails(built.details());
            dimension.setSortOrder(sortOrder++);
            dimension.setCreatedAt(LocalDateTime.now());
            matchDimensionMapper.insert(dimension);

            int subOrder = 0;
            for (SubDimensionVO sub : built.subDimensions()) {
                MatchSubDimension subDimension = new MatchSubDimension();
                subDimension.setDimensionId(dimension.getId());
                subDimension.setName(sub.getName());
                subDimension.setScore(sub.getScore());
                subDimension.setDescription(sub.getDescription());
                subDimension.setSortOrder(subOrder++);
                matchSubDimensionMapper.insert(subDimension);
            }
        }
        return analysis;
    }

    private MatchAnalysisResultVO toAnalysisResultVo(MatchAnalysis analysis) {
        List<MatchDimension> dimensionRows = matchDimensionMapper.selectList(
                new LambdaQueryWrapper<MatchDimension>()
                        .eq(MatchDimension::getAnalysisId, analysis.getId())
                        .orderByAsc(MatchDimension::getSortOrder)
        );

        List<DimensionScoreVO> dimensions = new ArrayList<>();
        for (MatchDimension row : dimensionRows) {
            DimensionScoreVO vo = new DimensionScoreVO();
            vo.setId(row.getId());
            vo.setName(row.getDimensionName());
            vo.setScore(row.getScore());
            vo.setWeight(row.getWeight());
            vo.setDescription(row.getDescription());
            vo.setDetails(row.getDetails());

            List<MatchSubDimension> subRows = matchSubDimensionMapper.selectList(
                    new LambdaQueryWrapper<MatchSubDimension>()
                            .eq(MatchSubDimension::getDimensionId, row.getId())
                            .orderByAsc(MatchSubDimension::getSortOrder)
            );
            if (!subRows.isEmpty()) {
                vo.setSubDimensions(subRows.stream().map(sub -> {
                    SubDimensionVO subVo = new SubDimensionVO();
                    subVo.setName(sub.getName());
                    subVo.setScore(sub.getScore());
                    subVo.setDescription(sub.getDescription());
                    return subVo;
                }).toList());
            }
            dimensions.add(vo);
        }

        MatchAnalysisResultVO vo = new MatchAnalysisResultVO();
        vo.setId(analysis.getId());
        vo.setResumeId(analysis.getResumeId());
        vo.setJobDescriptionId(analysis.getJobDescriptionId());
        vo.setMatchScore(analysis.getMatchScore());
        vo.setMatchLevel(analysis.getMatchLevel());
        vo.setDimensions(dimensions);
        vo.setAnalysis(analysis.getAnalysis());
        vo.setSummary(analysis.getSummary());
        vo.setHighlights(readStringList(analysis.getHighlights()));
        vo.setWeaknesses(readStringList(analysis.getWeaknesses()));
        vo.setSuggestions(readSuggestions(analysis.getSuggestions()));
        vo.setCreatedAt(analysis.getCreatedAt() == null ? null : analysis.getCreatedAt().format(FMT));
        vo.setUpdatedAt(analysis.getUpdatedAt() == null ? null : analysis.getUpdatedAt().format(FMT));
        vo.setVersion("1.0");
        return vo;
    }

    private List<BuiltDimension> buildDimensions(ParsedMatch parsed, Preset preset) {
        List<DimensionDef> defs = MatchDimensionConfig.dimensions(preset);
        List<BuiltDimension> result = new ArrayList<>();
        for (DimensionDef def : defs) {
            int score = MatchDimensionConfig.resolveScore(
                    def.getScoreSource(),
                    parsed.summaryScore(),
                    parsed.educationScore(),
                    parsed.experienceScore(),
                    parsed.skillScore(),
                    parsed.projectScore()
            );
            result.add(new BuiltDimension(def, clampScore(score), null, List.of()));
        }
        return result;
    }

    private int calculateRadarScore(List<BuiltDimension> dimensions) {
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (BuiltDimension dimension : dimensions) {
            BigDecimal weight = dimension.definition().getWeight();
            weighted = weighted.add(BigDecimal.valueOf(dimension.score()).multiply(weight));
            totalWeight = totalWeight.add(weight);
        }
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return weighted.divide(totalWeight, 0, RoundingMode.HALF_UP).intValue();
    }

    private String resolveMatchLevel(int score) {
        if (score >= 85) {
            return "excellent";
        }
        if (score >= 70) {
            return "good";
        }
        if (score >= 50) {
            return "average";
        }
        return "poor";
    }

    private String buildSummary(ParsedMatch parsed) {
        if (parsed.summary() != null && !parsed.summary().isBlank()) {
            return truncate(parsed.summary(), 500);
        }
        return truncate(parsed.analysis(), 500);
    }

    private List<String> deriveHighlights(List<BuiltDimension> dimensions) {
        return dimensions.stream()
                .filter(d -> d.score() >= 75)
                .map(d -> d.definition().getName() + "表现较好（" + d.score() + "分）")
                .toList();
    }

    private List<String> deriveWeaknesses(List<BuiltDimension> dimensions) {
        return dimensions.stream()
                .filter(d -> d.score() < 60)
                .map(d -> d.definition().getName() + "有待提升（" + d.score() + "分）")
                .toList();
    }

    private List<SuggestionVO> defaultSuggestions(String analysis) {
        SuggestionVO suggestion = new SuggestionVO();
        suggestion.setType("improve");
        suggestion.setTitle("整体优化建议");
        suggestion.setDescription(analysis);
        suggestion.setPriority("medium");
        suggestion.setCategory("general");
        return List.of(suggestion);
    }

    private Preset resolvePreset(String value) {
        if (value == null) {
            return Preset.GENERAL;
        }
        return switch (value.trim().toLowerCase()) {
            case "technical" -> Preset.TECHNICAL;
            case "management" -> Preset.MANAGEMENT;
            default -> Preset.GENERAL;
        };
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize JSON: {}", e.getMessage());
            return "[]";
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SuggestionVO> readSuggestions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, SUGGESTION_LIST_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen);
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
            String summary = node.path("summary").asText("");

            JsonNode suggestionsNode = node.get("suggestions");
            List<SuggestionVO> suggestions;
            String analysis;
            if (suggestionsNode != null && suggestionsNode.isArray()) {
                suggestions = parseSuggestions(suggestionsNode);
                analysis = node.path("analysis").asText("");
                if (analysis.isBlank()) {
                    analysis = suggestions.stream()
                            .map(SuggestionVO::getDescription)
                            .filter(desc -> desc != null && !desc.isBlank())
                            .reduce((a, b) -> a + "\n" + b)
                            .orElse("");
                }
            } else if (suggestionsNode != null && suggestionsNode.isTextual()) {
                analysis = suggestionsNode.asText("");
                suggestions = List.of();
            } else {
                analysis = node.path("analysis").asText("");
                suggestions = List.of();
            }

            if (analysis.isBlank()) {
                analysis = raw;
            }

            List<String> highlights = parseStringArray(node.get("highlights"));
            List<String> weaknesses = parseStringArray(node.get("weaknesses"));

            int legacyScore = calculateLegacyTotalScore(summaryScore, educationScore, experienceScore, skillScore, projectScore);
            return new ParsedMatch(
                    legacyScore, summaryScore, educationScore, experienceScore, skillScore, projectScore,
                    analysis.trim(), summary.trim(), highlights, weaknesses, suggestions
            );
        } catch (Exception e) {
            log.warn("Failed to parse AI result: {}", e.getMessage());
            return new ParsedMatch(60, 0, 0, 0, 0, 0, raw, "", List.of(), List.of(), List.of());
        }
    }

    private List<String> parseStringArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        node.forEach(item -> {
            if (item.isTextual()) {
                result.add(item.asText());
            }
        });
        return result;
    }

    private List<SuggestionVO> parseSuggestions(JsonNode node) {
        if (node == null) {
            return List.of();
        }
        if (node.isTextual()) {
            return List.of();
        }
        if (!node.isArray()) {
            return List.of();
        }
        List<SuggestionVO> result = new ArrayList<>();
        node.forEach(item -> {
            if (!item.isObject()) {
                return;
            }
            SuggestionVO suggestion = new SuggestionVO();
            suggestion.setType(item.path("type").asText("improve"));
            suggestion.setTitle(item.path("title").asText("优化建议"));
            suggestion.setDescription(item.path("description").asText(""));
            suggestion.setPriority(item.path("priority").asText("medium"));
            if (item.has("category")) {
                suggestion.setCategory(item.path("category").asText());
            }
            if (!suggestion.getDescription().isBlank()) {
                result.add(suggestion);
            }
        });
        return result;
    }

    private int calculateLegacyTotalScore(int summary, int education, int experience, int skill, int project) {
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
        vo.setAnalysisId(record.getAnalysisId());
        vo.setAnalysis(record.getAnalysis());
        vo.setCreatedAt(record.getCreatedAt() == null ? null : record.getCreatedAt().format(FMT));
        return vo;
    }

    private record BuiltDimension(DimensionDef definition, int score, String details, List<SubDimensionVO> subDimensions) {
    }

    private record ParsedMatch(
            int score,
            int summaryScore,
            int educationScore,
            int experienceScore,
            int skillScore,
            int projectScore,
            String analysis,
            String summary,
            List<String> highlights,
            List<String> weaknesses,
            List<SuggestionVO> suggestions
    ) {
        ParsedMatch {
            highlights = highlights == null ? List.of() : List.copyOf(highlights);
            weaknesses = weaknesses == null ? List.of() : List.copyOf(weaknesses);
            suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
        }
    }
}
