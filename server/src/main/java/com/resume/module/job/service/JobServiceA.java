package com.resume.module.job.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.BusinessException;
import com.resume.module.job.dto.JobRecommendationVO;
import com.resume.module.job.dto.JobSearchResultDTO;
import com.resume.module.job.entity.TargetJob;
import com.resume.module.job.mapper.TargetJobMapper;
import com.resume.module.job.util.UrlChecker;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeDetail;
import com.resume.module.resume.mapper.ResumeDetailMapper;
import com.resume.module.resume.mapper.ResumeMapper;
import jakarta.annotation.PreDestroy;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceA {

    private final ResumeMapper resumeMapper;
    private final ResumeDetailMapper resumeDetailMapper;
    private final TargetJobMapper targetJobMapper;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final UrlChecker urlChecker;
    private final RestClient restClient = RestClient.create();
    private final ExecutorService urlCheckExecutor = Executors.newFixedThreadPool(5);

    @Value("${app.ai.doubao.api-key:}")
    private String apiKey;

    @Value("${app.ai.doubao.base-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String baseUrl;

    @Value("${app.ai.doubao.model:doubao-seed-2-1-pro-260628}")
    private String model;

    private static final int MIN_MATCH_SCORE = 30;
    private static final TypeReference<List<JobSearchResultDTO>> SEARCH_RESULT_LIST_TYPE = new TypeReference<>() {};

    public List<JobRecommendationVO> searchJobs(Long userId, Long resumeId) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        if (resumeId == null) {
            throw new BusinessException(400, "简历ID不能为空");
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
            log.warn("豆包API Key未配置，路径A返回空列表");
            return List.of();
        }

        try {
            String promptTemplate = loadSearchPromptTemplate();
            String prompt = promptTemplate.replace("{resume_content}", resumeContent);

            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", List.of(Map.of(
                            "role", "user",
                            "content", prompt
                    )),
                    "tools", List.of(Map.of(
                            "type", "web_search"
                    ))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            log.info("开始请求豆包AI，userId={}, resumeId={}", userId, resumeId);
            long startTime = System.currentTimeMillis();

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restClient.post()
                    .uri(baseUrl + "/responses")
                    .headers(h -> h.addAll(headers))
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            long duration = System.currentTimeMillis() - startTime;
            log.info("收到豆包AI响应，用时{}ms，userId={}, resumeId={}", duration, userId, resumeId);
            log.info("JobServiceA API response: {}", resp);

            String content = parseResponseContent(resp);
            if (content == null || content.isBlank()) {
                log.warn("AI搜索返回内容为空");
                return List.of();
            }

            List<JobRecommendationVO> results = parseSearchResults(content.trim());
            List<JobRecommendationVO> filteredResults = filterResults(results);
            saveJobsToDatabase(filteredResults, userId);
            return filteredResults;

        } catch (RestClientResponseException e) {
            log.warn("JobServiceA API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            log.warn("JobServiceA search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildResumeContent(Long resumeId) {
        List<ResumeDetail> details = resumeDetailMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResumeDetail>()
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

    private String loadSearchPromptTemplate() {
        try {
            Resource resource = resourceLoader.getResource("classpath:prompts/job_recommend_search.txt");
            if (!resource.exists()) {
                throw new BusinessException(500, "提示词文件不存在");
            }
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to load search prompt template: {}", e.getMessage());
            throw new BusinessException(500, "加载提示词文件失败");
        }
    }

    private String parseResponseContent(Map<String, Object> resp) {
        if (resp == null) {
            return null;
        }
        Object outputObj = resp.get("output");
        if (!(outputObj instanceof List<?> outputList)) {
            log.warn("Unexpected output format: expected List, got {}", outputObj != null ? outputObj.getClass().getName() : "null");
            return null;
        }

        for (Object item : outputList) {
            if (!(item instanceof Map<?, ?> itemMap)) {
                continue;
            }
            Object type = itemMap.get("type");
            if (!"message".equals(type)) {
                continue;
            }

            Object contentObj = itemMap.get("content");
            if (!(contentObj instanceof List<?> contentList)) {
                continue;
            }

            for (Object contentItem : contentList) {
                if (!(contentItem instanceof Map<?, ?> contentMap)) {
                    continue;
                }
                Object contentType = contentMap.get("type");
                if (!"output_text".equals(contentType)) {
                    continue;
                }
                Object text = contentMap.get("text");
                return text == null ? null : text.toString();
            }
        }

        log.warn("Could not find message/output_text in response");
        return null;
    }

    private List<JobRecommendationVO> parseSearchResults(String raw) {
        try {
            String json = extractJsonArray(raw);
            if (json == null || json.isBlank()) {
                return List.of();
            }

            List<JobSearchResultDTO> dtoList = objectMapper.readValue(json, SEARCH_RESULT_LIST_TYPE);
            List<JobRecommendationVO> results = new ArrayList<>();

            for (JobSearchResultDTO dto : dtoList) {
                JobRecommendationVO vo = new JobRecommendationVO();
                vo.setJobName(dto.getJobName());
                vo.setJdContent(dto.getJdContent());
                vo.setSource("NETWORK");
                vo.setSourceUrl(dto.getSourceUrl());

                ParsedMatchScore parsed = calculateMatchScore(dto);
                vo.setMatchScore(parsed.matchScore());
                vo.setMatchReason(parsed.matchReason());

                results.add(vo);
            }

            return results;
        } catch (Exception e) {
            log.warn("Failed to parse search results: {}", e.getMessage());
            return List.of();
        }
    }

    private ParsedMatchScore calculateMatchScore(JobSearchResultDTO dto) {
        int summaryScore = clampScore(dto.getSummary_score());
        int educationScore = clampScore(dto.getEducation_score());
        int experienceScore = clampScore(dto.getExperience_score());
        int skillScore = clampScore(dto.getSkill_score());
        int projectScore = clampScore(dto.getProject_score());

        int totalScore = calculateTotalScore(summaryScore, educationScore, experienceScore, skillScore, projectScore);

        String matchReason = dto.getMatchReason();
        if (matchReason == null || matchReason.isBlank()) {
            matchReason = String.format("匹配分析：个人总结(%d分)、教育背景(%d分)、工作经历(%d分)、专业技能(%d分)、项目经历(%d分)。综合评分%d分。",
                    summaryScore, educationScore, experienceScore, skillScore, projectScore, totalScore);
        }

        return new ParsedMatchScore(totalScore, matchReason);
    }

    private int clampScore(Integer score) {
        if (score == null || score < 0) {
            return 0;
        }
        return Math.min(score, 100);
    }

    private int calculateTotalScore(int summary, int education, int experience, int skill, int project) {
        double total = summary * 0.10 + education * 0.15 + experience * 0.25 + skill * 0.25 + project * 0.25;
        return (int) Math.round(total);
    }

    private String extractJsonArray(String raw) {
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        int objectStart = raw.indexOf('{');
        int objectEnd = raw.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return "[" + raw.substring(objectStart, objectEnd + 1) + "]";
        }
        return null;
    }

    private List<JobRecommendationVO> filterResults(List<JobRecommendationVO> results) {
        List<JobRecommendationVO> filtered = new ArrayList<>();
        for (JobRecommendationVO vo : results) {
            if (vo.getMatchScore() == null || vo.getMatchScore() < MIN_MATCH_SCORE) {
                continue;
            }
            if (vo.getSourceUrl() == null || vo.getSourceUrl().isBlank()) {
                continue;
            }
            if (vo.getJobName() == null || vo.getJobName().isBlank()) {
                continue;
            }
            filtered.add(vo);
        }

        if (filtered.isEmpty()) {
            return filtered;
        }

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (JobRecommendationVO vo : filtered) {
            futures.add(CompletableFuture.supplyAsync(
                    () -> urlChecker.isUrlAccessible(vo.getSourceUrl()),
                    urlCheckExecutor
            ));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<JobRecommendationVO> accessibleResults = new ArrayList<>();
        for (int i = 0; i < filtered.size(); i++) {
            boolean accessible = futures.get(i).join();
            if (accessible) {
                accessibleResults.add(filtered.get(i));
            } else {
                log.debug("过滤掉不可访问的链接: {}", filtered.get(i).getSourceUrl());
            }
        }

        return accessibleResults;
    }

    private void saveJobsToDatabase(List<JobRecommendationVO> results, Long userId) {
        for (JobRecommendationVO vo : results) {
            try {
                TargetJob existing = targetJobMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TargetJob>()
                                .eq(TargetJob::getSourceUrls, vo.getSourceUrl())
                );
                if (existing != null) {
                    continue;
                }

                TargetJob job = new TargetJob();
                job.setUserId(userId);
                job.setJobName(vo.getJobName());
                job.setJdContent(vo.getJdContent());
                job.setSource(1);
                job.setSourceUrls(vo.getSourceUrl());
                targetJobMapper.insert(job);
            } catch (Exception e) {
                log.warn("Failed to save job to database: {}", e.getMessage());
            }
        }
    }

    @PreDestroy
    public void destroy() {
        urlCheckExecutor.shutdown();
    }

    private record ParsedMatchScore(int matchScore, String matchReason) {
    }
}