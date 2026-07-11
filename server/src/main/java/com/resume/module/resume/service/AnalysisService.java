package com.resume.module.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.BusinessException;
import com.resume.module.analyze.util.AnalyzeUtil;
import com.resume.module.resume.dto.AnalysisResult;
import com.resume.module.resume.entity.AnalysisRecord;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeDetail;
import com.resume.module.resume.mapper.AnalysisRecordMapper;
import com.resume.module.resume.mapper.ResumeDetailMapper;
import com.resume.module.resume.mapper.ResumeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Map<String, Double> WEIGHTS = Map.of(
            "summary", 0.10,
            "education", 0.15,
            "experience", 0.25,
            "skill", 0.25,
            "project", 0.25
    );

    private final AnalysisRecordMapper analysisRecordMapper;
    private final ResumeMapper resumeMapper;
    private final ResumeDetailMapper resumeDetailMapper;
    private final ObjectMapper objectMapper;
    private final AnalyzeUtil analyzeUtil;

    @Value("${dashscope-maas.api-key:}")
    private String maasApiKey;

    @Transactional
    public AnalysisResult analyzeResume(Long userId, Long resumeId) {
        Resume resume = getOwnedResume(resumeId, userId);
        String requestId = UUID.randomUUID().toString().replace("-", "");

        AnalysisRecord record = new AnalysisRecord();
        record.setUserId(userId);
        record.setResumeId(resumeId);
        record.setStatus(0);
        record.setRequestId(requestId);
        analysisRecordMapper.insert(record);

        try {
            if (maasApiKey == null || maasApiKey.isBlank()) {
                throw new BusinessException(500, "AI 分析服务未配置");
            }

            String resumeContent = buildResumeContent(resumeId);
            String aiResponse = analyzeUtil.getAIResponse(resumeContent);
            JsonNode parsed = parseAiJson(aiResponse);

            Map<String, Integer> scores = extractScores(parsed);
            int totalScore = calculateTotalScore(scores);
            String suggestions = parsed.path("suggestions").asText("");

            record.setSummaryScore(scores.get("summary"));
            record.setEducationScore(scores.get("education"));
            record.setExperienceScore(scores.get("experience"));
            record.setSkillScore(scores.get("skill"));
            record.setProjectScore(scores.get("project"));
            record.setTotalScore(totalScore);
            record.setSuggestions(suggestions);
            record.setStatus(1);
            record.setErrorMessage(null);
            analysisRecordMapper.updateById(record);

            return toResult(record, resume.getTitle());
        } catch (BusinessException e) {
            markFailed(record, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Resume analysis failed, requestId={}", requestId, e);
            markFailed(record, e.getMessage());
            throw new BusinessException(500, "简历分析失败: " + e.getMessage());
        }
    }

    public Map<String, Object> listRecords(Long userId, int page, int size) {
        if (page < 1) {
            throw new BusinessException(400, "页码必须大于 0");
        }
        if (size < 1 || size > 100) {
            throw new BusinessException(400, "每页条数必须在 1-100 之间");
        }

        Page<AnalysisRecord> pageQuery = new Page<>(page, size);
        Page<AnalysisRecord> result = analysisRecordMapper.selectPage(
                pageQuery,
                new LambdaQueryWrapper<AnalysisRecord>()
                        .eq(AnalysisRecord::getUserId, userId)
                        .orderByDesc(AnalysisRecord::getCreatedAt)
        );

        Map<Long, String> resumeTitles = loadResumeTitles(result.getRecords());
        List<AnalysisResult> records = result.getRecords().stream()
                .map(record -> toResult(record, resumeTitles.get(record.getResumeId())))
                .toList();

        Map<String, Object> payload = new HashMap<>();
        payload.put("records", records);
        payload.put("total", result.getTotal());
        payload.put("page", result.getCurrent());
        payload.put("size", result.getSize());
        return payload;
    }

    public AnalysisResult getRecord(Long recordId, Long userId) {
        AnalysisRecord record = analysisRecordMapper.selectById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(404, "分析记录不存在");
        }

        Resume resume = record.getResumeId() == null ? null : resumeMapper.selectById(record.getResumeId());
        return toResult(record, resume == null ? null : resume.getTitle());
    }

    private Resume getOwnedResume(Long resumeId, Long userId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BusinessException(404, "简历不存在");
        }
        return resume;
    }

    private Map<Long, String> loadResumeTitles(List<AnalysisRecord> records) {
        Map<Long, String> titles = new HashMap<>();
        for (AnalysisRecord record : records) {
            if (record.getResumeId() == null || titles.containsKey(record.getResumeId())) {
                continue;
            }
            Resume resume = resumeMapper.selectById(record.getResumeId());
            titles.put(record.getResumeId(), resume == null ? null : resume.getTitle());
        }
        return titles;
    }

    private String buildResumeContent(Long resumeId) {
        List<ResumeDetail> details = resumeDetailMapper.selectList(
                new LambdaQueryWrapper<ResumeDetail>()
                        .eq(ResumeDetail::getResumeId, resumeId)
                        .orderByAsc(ResumeDetail::getSortOrder)
        );
        if (details.isEmpty()) {
            throw new BusinessException(400, "简历内容为空，请先完善简历后再分析");
        }

        StringBuilder builder = new StringBuilder();
        for (ResumeDetail detail : details) {
            builder.append("【").append(detail.getSectionName()).append("】\n");
            builder.append(detail.getContent()).append("\n\n");
        }
        return builder.toString();
    }

    JsonNode parseAiJson(String rawContent) throws Exception {
        String jsonText = rawContent.trim();
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(jsonText);
        if (matcher.find()) {
            jsonText = matcher.group(1).trim();
        } else {
            int start = jsonText.indexOf('{');
            int end = jsonText.lastIndexOf('}');
            if (start >= 0 && end > start) {
                jsonText = jsonText.substring(start, end + 1);
            }
        }

        JsonNode node = objectMapper.readTree(jsonText);
        if (!hasScoreFields(node)) {
            throw new IllegalArgumentException("AI 返回 JSON 缺少评分字段");
        }
        return node;
    }

    private boolean hasScoreFields(JsonNode node) {
        if (node.has("scores")) {
            return true;
        }
        return node.has("summary_score")
                && node.has("education_score")
                && node.has("experience_score")
                && node.has("skill_score")
                && node.has("project_score");
    }

    private Map<String, Integer> extractScores(JsonNode parsed) {
        JsonNode scoresNode = parsed.has("scores") ? parsed.get("scores") : parsed;
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (String key : WEIGHTS.keySet()) {
            JsonNode value = scoresNode.get(key);
            if (value == null || !value.isInt()) {
                value = parsed.get(key + "_score");
            }
            if (value == null || !value.isInt()) {
                throw new IllegalArgumentException("AI 返回 JSON 缺少或格式错误: " + key);
            }
            int score = value.asInt();
            if (score < 0 || score > 100) {
                throw new IllegalArgumentException("评分必须在 0-100 之间: " + key);
            }
            scores.put(key, score);
        }
        return scores;
    }

    private int calculateTotalScore(Map<String, Integer> scores) {
        double total = 0;
        for (Map.Entry<String, Double> entry : WEIGHTS.entrySet()) {
            total += scores.get(entry.getKey()) * entry.getValue();
        }
        return (int) Math.round(total);
    }

    private void markFailed(AnalysisRecord record, String message) {
        record.setStatus(2);
        record.setErrorMessage(message == null ? "未知错误" : message);
        analysisRecordMapper.updateById(record);
    }

    private AnalysisResult toResult(AnalysisRecord record, String resumeTitle) {
        AnalysisResult result = new AnalysisResult();
        result.setId(record.getId());
        result.setResumeId(record.getResumeId());
        result.setResumeTitle(resumeTitle);
        result.setScores(Map.of(
                "summary", safeScore(record.getSummaryScore()),
                "education", safeScore(record.getEducationScore()),
                "experience", safeScore(record.getExperienceScore()),
                "skill", safeScore(record.getSkillScore()),
                "project", safeScore(record.getProjectScore())
        ));
        result.setTotalScore(record.getTotalScore());
        result.setSuggestions(record.getSuggestions());
        result.setStatus(statusLabel(record.getStatus()));
        result.setCreatedAt(record.getCreatedAt() == null ? null : record.getCreatedAt().format(DATE_TIME_FORMATTER));
        return result;
    }

    private int safeScore(Integer score) {
        return score == null ? 0 : score;
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "unknown";
        }
        return switch (status) {
            case 0 -> "processing";
            case 1 -> "completed";
            case 2 -> "failed";
            default -> "unknown";
        };
    }
}
