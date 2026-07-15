package com.resume.module.job.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.BusinessException;
import com.resume.module.job.dto.*;
import com.resume.module.job.entity.MatchRecommendation;
import com.resume.module.job.entity.RecommendationSession;
import com.resume.module.job.mapper.MatchRecommendationMapper;
import com.resume.module.job.mapper.RecommendationSessionMapper;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeDetail;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FindJobService {

    private final ResumeMapper resumeMapper;
    private final ResumeDetailMapper resumeDetailMapper;
    private final JobServiceA jobServiceA;
    private final JobServiceB jobServiceB;
    private final RecommendationSessionMapper sessionMapper;
    private final MatchRecommendationMapper recommendationMapper;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final RestClient restClient = RestClient.create();

    @Value("${app.ai.qwen.api-key:}")
    private String qwenApiKey;

    @Value("${app.ai.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String qwenBaseUrl;

    @Value("${app.ai.qwen.model:qwen-plus}")
    private String qwenModel;

    private static final int MIN_MATCH_SCORE = 30;
    private static final int SOURCE_NETWORK = 1;
    private static final int SOURCE_PLATFORM = 2;

    public RecommendationResultVO recommendJobs(Long userId, Long resumeId, Boolean enableWebSearch) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        if (resumeId == null) {
            throw new BusinessException(400, "简历ID不能为空");
        }
        if (enableWebSearch == null) {
            enableWebSearch = true;
        }

        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new BusinessException(404, "简历不存在");
        }
        if (!resume.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该简历");
        }

        String resumeContent = buildResumeContent(resumeId);
        if (resumeContent.isBlank()) {
            throw new BusinessException(400, "简历内容为空，请先完善简历");
        }

        List<JobRecommendationVO> resultsA = List.of();
        List<JobRecommendationVO> resultsB = List.of();

        if (enableWebSearch) {
            CompletableFuture<List<JobRecommendationVO>> futureA = CompletableFuture.supplyAsync(
                    () -> jobServiceA.searchJobs(userId, resumeId)
            ).exceptionally(e -> {
                log.warn("JobServiceA search failed: {}", e.getMessage());
                return List.of();
            });

            CompletableFuture<List<JobRecommendationVO>> futureB = CompletableFuture.supplyAsync(
                    () -> jobServiceB.matchJobs(userId, resumeId)
            ).exceptionally(e -> {
                log.warn("JobServiceB match failed: {}", e.getMessage());
                return List.of();
            });

            CompletableFuture.allOf(futureA, futureB).join();

            resultsA = futureA.join();
            resultsB = futureB.join();
        } else {
            try {
                resultsB = jobServiceB.matchJobs(userId, resumeId);
            } catch (Exception e) {
                log.warn("JobServiceB match failed: {}", e.getMessage());
                resultsB = List.of();
            }
        }

        List<JobRecommendationVO> allResults = new ArrayList<>();
        if (resultsA != null) {
            allResults.addAll(resultsA);
        }
        if (resultsB != null) {
            allResults.addAll(resultsB);
        }

        List<JobRecommendationVO> filteredResults = allResults.stream()
                .filter(vo -> vo.getMatchScore() != null && vo.getMatchScore() >= MIN_MATCH_SCORE)
                .sorted(Comparator.comparingInt(JobRecommendationVO::getMatchScore).reversed())
                .collect(Collectors.toList());

        ImprovementSuggestionVO improvementSuggestion = null;
        if (filteredResults.isEmpty()) {
            if (enableWebSearch) {
                improvementSuggestion = generateImprovementSuggestion(resumeContent);
            } else {
                improvementSuggestion = createPlatformOnlyNoResultSuggestion();
            }
        }

        RecommendationSession session = saveSessionWithTransaction(userId, resumeId, !filteredResults.isEmpty(), improvementSuggestion);

        if (!filteredResults.isEmpty()) {
            saveRecommendationsWithTransaction(session.getId(), userId, resumeId, filteredResults);
        }

        RecommendationResultVO result = new RecommendationResultVO();
        result.setResumeId(resumeId);
        result.setRecommendations(filteredResults);
        result.setImprovementSuggestion(improvementSuggestion);
        result.setCreatedAt(session.getCreatedAt());

        return result;
    }

    private ImprovementSuggestionVO createPlatformOnlyNoResultSuggestion() {
        ImprovementSuggestionVO vo = new ImprovementSuggestionVO();
        vo.setProblemDescription("当前平台内没有找到合适的岗位");
        vo.setSuggestions(List.of("建议开启联网搜索，获取更多外部岗位信息", "可尝试完善简历后再次匹配"));
        vo.setPriority("medium");
        return vo;
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

    private ImprovementSuggestionVO generateImprovementSuggestion(String resumeContent) {
        if (qwenApiKey == null || qwenApiKey.isBlank()) {
            log.warn("Qwen API Key未配置，无法生成改进建议");
            return null;
        }

        try {
            String promptTemplate = loadImprovementPromptTemplate();
            String prompt = promptTemplate.replace("{resume_content}", resumeContent);

            Map<String, Object> body = Map.of(
                    "model", qwenModel,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(qwenApiKey);

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restClient.post()
                    .uri(qwenBaseUrl + "/chat/completions")
                    .headers(h -> h.addAll(headers))
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String content = parseChatContent(resp);
            if (content == null || content.isBlank()) {
                log.warn("AI改进建议返回内容为空");
                return null;
            }

            return parseImprovementSuggestion(content.trim());
        } catch (RestClientResponseException e) {
            log.warn("Improvement suggestion API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.warn("Failed to generate improvement suggestion: {}", e.getMessage());
            return null;
        }
    }

    private String loadImprovementPromptTemplate() {
        try {
            Resource resource = resourceLoader.getResource("classpath:prompts/resume_improvement_suggestion.txt");
            if (!resource.exists()) {
                throw new BusinessException(500, "提示词文件不存在");
            }
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to load improvement prompt template: {}", e.getMessage());
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

    private ImprovementSuggestionVO parseImprovementSuggestion(String raw) {
        try {
            String json = extractJsonObject(raw);
            JsonNode node = objectMapper.readTree(json);

            ImprovementSuggestionVO vo = new ImprovementSuggestionVO();
            vo.setProblemDescription(node.path("problemDescription").asText(""));

            List<String> suggestions = new ArrayList<>();
            JsonNode suggestionsNode = node.path("suggestions");
            if (suggestionsNode.isArray()) {
                for (JsonNode item : suggestionsNode) {
                    suggestions.add(item.asText());
                }
            }
            vo.setSuggestions(suggestions);
            vo.setPriority(node.path("priority").asText("medium"));

            return vo;
        } catch (Exception e) {
            log.warn("Failed to parse improvement suggestion: {}", e.getMessage());
            return null;
        }
    }

    private String extractJsonObject(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    @Transactional
    public RecommendationSession saveSessionWithTransaction(Long userId, Long resumeId, boolean hasMatchResult, ImprovementSuggestionVO suggestion) {
        RecommendationSession session = new RecommendationSession();
        session.setUserId(userId);
        session.setResumeId(resumeId);
        session.setHasMatchResult(hasMatchResult ? 1 : 0);

        if (!hasMatchResult && suggestion != null) {
            session.setProblemDescription(suggestion.getProblemDescription());
            session.setSuggestionPriority(suggestion.getPriority());
            try {
                session.setImprovementSuggestions(objectMapper.writeValueAsString(suggestion.getSuggestions()));
            } catch (Exception e) {
                log.warn("Failed to serialize suggestions: {}", e.getMessage());
            }
        }

        sessionMapper.insert(session);
        return session;
    }

    @Transactional
    public void saveRecommendationsWithTransaction(Long sessionId, Long userId, Long resumeId, List<JobRecommendationVO> recommendations) {
        for (JobRecommendationVO vo : recommendations) {
            MatchRecommendation record = new MatchRecommendation();
            record.setSessionId(sessionId);
            record.setUserId(userId);
            record.setResumeId(resumeId);
            record.setJobId(vo.getSource().equals("PLATFORM") ? vo.getSourceJobId() : null);
            record.setJobName(vo.getJobName());
            record.setJdContent(vo.getJdContent());
            record.setMatchScore(vo.getMatchScore());
            record.setMatchReason(vo.getMatchReason());
            record.setSource(vo.getSource().equals("NETWORK") ? SOURCE_NETWORK : SOURCE_PLATFORM);
            record.setSourceUrl(vo.getSourceUrl());
            record.setSourceJobId(vo.getSourceJobId());

            recommendationMapper.insert(record);
        }
    }

    public PageResult<RecommendationHistoryVO> listRecommendations(Long userId, int page, int size) {
        Page<MatchRecommendation> pageQuery = new Page<>(page, size);

        LambdaQueryWrapper<MatchRecommendation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MatchRecommendation::getUserId, userId)
                .orderByDesc(MatchRecommendation::getCreatedAt);

        Page<MatchRecommendation> result = recommendationMapper.selectPage(pageQuery, wrapper);

        List<RecommendationHistoryVO> content = result.getRecords().stream()
                .map(this::toHistoryVO)
                .collect(Collectors.toList());

        return new PageResult<>(content, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private RecommendationHistoryVO toHistoryVO(MatchRecommendation record) {
        RecommendationHistoryVO vo = new RecommendationHistoryVO();
        vo.setId(record.getId());
        vo.setResumeId(record.getResumeId());
        vo.setJobName(record.getJobName());
        vo.setMatchScore(record.getMatchScore());
        vo.setSource(record.getSource());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }

    public RecommendationDetailVO getRecommendationDetail(Long userId, Long id) {
        MatchRecommendation record = recommendationMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "推荐记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该记录");
        }

        RecommendationDetailVO vo = new RecommendationDetailVO();
        vo.setId(record.getId());
        vo.setResumeId(record.getResumeId());
        vo.setJobId(record.getJobId());
        vo.setJobName(record.getJobName());
        vo.setJdContent(record.getJdContent());
        vo.setMatchScore(record.getMatchScore());
        vo.setMatchReason(record.getMatchReason());
        vo.setSource(record.getSource());
        vo.setSourceUrl(record.getSourceUrl());
        vo.setSourceJobId(record.getSourceJobId());
        vo.setCreatedAt(record.getCreatedAt());

        RecommendationSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<RecommendationSession>()
                        .eq(RecommendationSession::getId, record.getSessionId())
        );

        if (session != null && session.getHasMatchResult() != null && session.getHasMatchResult() == 0) {
            vo.setHasImprovementSuggestion(1);
            if (session.getProblemDescription() != null) {
                ImprovementSuggestionVO suggestion = new ImprovementSuggestionVO();
                suggestion.setProblemDescription(session.getProblemDescription());
                suggestion.setPriority(session.getSuggestionPriority());
                if (session.getImprovementSuggestions() != null) {
                    try {
                        List<String> suggestions = objectMapper.readValue(
                                session.getImprovementSuggestions(),
                                new TypeReference<List<String>>() {}
                        );
                        suggestion.setSuggestions(suggestions);
                    } catch (Exception e) {
                        log.warn("Failed to parse improvement suggestions: {}", e.getMessage());
                    }
                }
                vo.setImprovementSuggestion(suggestion);
            }
        } else {
            vo.setHasImprovementSuggestion(0);
        }

        return vo;
    }
}