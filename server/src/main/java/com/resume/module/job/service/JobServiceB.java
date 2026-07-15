package com.resume.module.job.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.BusinessException;
import com.resume.common.TextEmbeddingService;
import com.resume.module.job.dto.JobRecommendationVO;
import com.resume.module.job.entity.JobSemanticVector;
import com.resume.module.job.entity.TargetJob;
import com.resume.module.job.mapper.JobSemanticVectorMapper;
import com.resume.module.job.mapper.TargetJobMapper;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeDetail;
import com.resume.module.resume.entity.ResumeSemanticVector;
import com.resume.module.resume.mapper.ResumeDetailMapper;
import com.resume.module.resume.mapper.ResumeMapper;
import com.resume.module.resume.mapper.ResumeSemanticVectorMapper;
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
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceB {

    private final TargetJobMapper targetJobMapper;
    private final JobSemanticVectorMapper jobSemanticVectorMapper;
    private final ResumeMapper resumeMapper;
    private final ResumeDetailMapper resumeDetailMapper;
    private final ResumeSemanticVectorMapper resumeSemanticVectorMapper;
    private final TextEmbeddingService textEmbeddingService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();
    private final ResourceLoader resourceLoader;

    @Value("${app.ai.qwen.api-key:}")
    private String apiKey;

    @Value("${app.ai.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${app.ai.qwen.model:qwen-plus}")
    private String model;

    private static final int TOP_N = 2;
    private static final int MIN_MATCH_SCORE = 30;

    @Transactional
    public List<JobRecommendationVO> matchJobs(Long userId, Long resumeId) {
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

        List<TargetJob> allJobs = targetJobMapper.selectList(new LambdaQueryWrapper<>());
        if (allJobs.isEmpty()) {
            return List.of();
        }

        List<Double> resumeVector = getOrGenerateResumeVector(resumeId, resumeContent);

        Map<Long, List<Double>> jobVectors = getOrGenerateJobVectors(allJobs);

        List<JobScore> scoredJobs = calculateSimilarities(resumeVector, allJobs, jobVectors);

        List<JobScore> topJobs = scoredJobs.stream()
                .sorted(Comparator.comparingDouble(JobScore::similarity).reversed())
                .limit(TOP_N)
                .collect(Collectors.toList());

        if (topJobs.isEmpty()) {
            return List.of();
        }

        List<JobRecommendationVO> recommendations = aiScoreValidation(resumeContent, topJobs);

        return recommendations.stream()
                .filter(vo -> vo.getMatchScore() >= MIN_MATCH_SCORE)
                .collect(Collectors.toList());
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

    private List<Double> getOrGenerateResumeVector(Long resumeId, String resumeContent) {
        ResumeSemanticVector existing = resumeSemanticVectorMapper.selectOne(
                new LambdaQueryWrapper<ResumeSemanticVector>()
                        .eq(ResumeSemanticVector::getResumeId, resumeId)
        );

        if (existing != null && existing.getModifiedFlag() != null && existing.getModifiedFlag() == 0) {
            log.info("Using cached resume vector for resumeId: {}", resumeId);
            return textEmbeddingService.parseVectorJson(existing.getVector());
        }

        log.info("Generating new resume vector for resumeId: {}", resumeId);
        List<Double> vector = textEmbeddingService.embed(resumeContent);
        String vectorJson = textEmbeddingService.embedToJson(resumeContent);

        saveResumeVector(resumeId, vectorJson);

        return vector;
    }

    private void saveResumeVector(Long resumeId, String vectorJson) {
        ResumeSemanticVector existing = resumeSemanticVectorMapper.selectOne(
                new LambdaQueryWrapper<ResumeSemanticVector>()
                        .eq(ResumeSemanticVector::getResumeId, resumeId)
        );

        if (existing != null) {
            existing.setVector(vectorJson);
            existing.setModifiedFlag(0);
            existing.setUpdatedAt(LocalDateTime.now());
            resumeSemanticVectorMapper.updateById(existing);
        } else {
            ResumeSemanticVector newVector = new ResumeSemanticVector();
            newVector.setResumeId(resumeId);
            newVector.setVector(vectorJson);
            newVector.setModifiedFlag(0);
            resumeSemanticVectorMapper.insert(newVector);
        }
    }

    private Map<Long, List<Double>> getOrGenerateJobVectors(List<TargetJob> jobs) {
        Map<Long, List<Double>> result = new HashMap<>();

        List<Long> jobIds = jobs.stream().map(TargetJob::getId).collect(Collectors.toList());
        List<JobSemanticVector> existingVectors = jobSemanticVectorMapper.selectList(
                new LambdaQueryWrapper<JobSemanticVector>()
                        .in(JobSemanticVector::getJobId, jobIds)
        );

        Map<Long, JobSemanticVector> cachedMap = existingVectors.stream()
                .filter(v -> v.getModifiedFlag() != null && v.getModifiedFlag() == 0)
                .collect(Collectors.toMap(JobSemanticVector::getJobId, v -> v));

        List<TargetJob> needGenerate = jobs.stream()
                .filter(job -> !cachedMap.containsKey(job.getId()))
                .collect(Collectors.toList());

        for (Map.Entry<Long, JobSemanticVector> entry : cachedMap.entrySet()) {
            result.put(entry.getKey(), textEmbeddingService.parseVectorJson(entry.getValue().getVector()));
        }

        if (!needGenerate.isEmpty()) {
            log.info("Need to generate vectors for {} jobs", needGenerate.size());
            Map<Long, List<Double>> generatedVectors = generateBatchJobVectors(needGenerate);
            result.putAll(generatedVectors);
        }

        return result;
    }

    private static final long REQUEST_INTERVAL_MS = 1100;

    private Map<Long, List<Double>> generateBatchJobVectors(List<TargetJob> jobs) {
        Map<Long, List<Double>> result = new HashMap<>();

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(jobs.size(), 10));
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        log.info("开始生成文本向量，待处理岗位数={}", jobs.size());

        for (int i = 0; i < jobs.size(); i++) {
            TargetJob job = jobs.get(i);
            int index = i;

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    long delay = index * REQUEST_INTERVAL_MS - (System.currentTimeMillis() - startTime);
                    if (delay > 0) {
                        Thread.sleep(delay);
                    }

                    String jdContent = job.getJdContent() != null ? job.getJdContent() : "";
                    if (jdContent.isBlank()) {
                        log.warn("Empty JD content for job {}", job.getId());
                        return;
                    }

                    List<Double> vector = textEmbeddingService.embed(jdContent);

                    if (vector.isEmpty()) {
                        log.warn("Empty vector generated for job {}", job.getId());
                        return;
                    }

                    synchronized (result) {
                        result.put(job.getId(), vector);
                    }

                    try {
                        String vectorJson = objectMapper.writeValueAsString(vector);
                        saveJobVector(job.getId(), vectorJson);
                    } catch (Exception e) {
                        log.warn("Failed to serialize vector for job {}: {}", job.getId(), e.getMessage());
                    }

                    log.debug("Generated vector for job {} at index {}", job.getId(), index);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Vector generation interrupted for job {}", job.getId());
                } catch (Exception e) {
                    log.warn("Failed to generate vector for job {}: {}", job.getId(), e.getMessage());
                }
            }, executor);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("生成文本向量完成，用时{}ms，成功生成{}个向量", duration, result.size());
        return result;
    }

    private void saveJobVector(Long jobId, String vectorJson) {
        JobSemanticVector existing = jobSemanticVectorMapper.selectOne(
                new LambdaQueryWrapper<JobSemanticVector>()
                        .eq(JobSemanticVector::getJobId, jobId)
        );

        if (existing != null) {
            existing.setVector(vectorJson);
            existing.setModifiedFlag(0);
            existing.setUpdatedAt(LocalDateTime.now());
            jobSemanticVectorMapper.updateById(existing);
        } else {
            JobSemanticVector newVector = new JobSemanticVector();
            newVector.setJobId(jobId);
            newVector.setVector(vectorJson);
            newVector.setModifiedFlag(0);
            jobSemanticVectorMapper.insert(newVector);
        }
    }

    private List<JobScore> calculateSimilarities(List<Double> resumeVector, List<TargetJob> jobs, Map<Long, List<Double>> jobVectors) {
        List<JobScore> result = new ArrayList<>();

        for (TargetJob job : jobs) {
            List<Double> jobVector = jobVectors.get(job.getId());
            if (jobVector == null || jobVector.isEmpty()) {
                continue;
            }

            double similarity = textEmbeddingService.calculateCosineSimilarity(resumeVector, jobVector);
            result.add(new JobScore(job, similarity));
        }

        return result;
    }

    private List<JobRecommendationVO> aiScoreValidation(String resumeContent, List<JobScore> topJobs) {
        List<JobRecommendationVO> result = new ArrayList<>();

        for (JobScore jobScore : topJobs) {
            TargetJob job = jobScore.job();
            String jdContent = job.getJdContent() != null ? job.getJdContent() : "";

            try {
                String aiResult = callAiForMatchScore(resumeContent, jdContent);
                ParsedMatchScore parsed = parseAiMatchScore(aiResult);

                JobRecommendationVO vo = new JobRecommendationVO();
                vo.setJobName(job.getJobName());
                vo.setJdContent(jdContent);
                vo.setMatchScore(parsed.matchScore());
                vo.setMatchReason(parsed.matchReason());
                vo.setSource("PLATFORM");
                vo.setSourceUrl(null);
                vo.setSourceJobId(job.getId());

                result.add(vo);
            } catch (Exception e) {
                log.warn("AI score validation failed for job {}: {}", job.getId(), e.getMessage());

                JobRecommendationVO vo = new JobRecommendationVO();
                vo.setJobName(job.getJobName());
                vo.setJdContent(jdContent);
                vo.setMatchScore((int) (jobScore.similarity() * 100));
                vo.setMatchReason("基于语义向量相似度计算");
                vo.setSource("PLATFORM");
                vo.setSourceUrl(null);
                vo.setSourceJobId(job.getId());

                result.add(vo);
            }
        }

        return result;
    }

    private String callAiForMatchScore(String resumeContent, String jdContent) {
        String promptTemplate = loadMatchPromptTemplate();
        String prompt = promptTemplate.replace("【在此处粘贴简历全文】", resumeContent)
                + "\n\n## 六、职位描述(JD)信息\n【职位描述】\n" + jdContent;

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            log.info("开始请求千问模型进行匹配评分");
            long startTime = System.currentTimeMillis();

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .headers(h -> h.addAll(headers))
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            long duration = System.currentTimeMillis() - startTime;
            log.info("收到千问模型响应，用时{}ms", duration);

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

    private String loadMatchPromptTemplate() {
        try {
            Resource resource = resourceLoader.getResource("classpath:prompts/job_match_score_prompt.txt");
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

    private ParsedMatchScore parseAiMatchScore(String raw) {
        String json = extractJsonObject(raw);
        try {
            JsonNode node = objectMapper.readTree(json);

            int summaryScore = clampScore(node.path("summary_score").asInt(-1));
            int educationScore = clampScore(node.path("education_score").asInt(-1));
            int experienceScore = clampScore(node.path("experience_score").asInt(-1));
            int skillScore = clampScore(node.path("skill_score").asInt(-1));
            int projectScore = clampScore(node.path("project_score").asInt(-1));

            int totalScore = calculateTotalScore(summaryScore, educationScore, experienceScore, skillScore, projectScore);

            String analysis = node.path("match_reason").asText("");
            String matchReason = truncate(analysis, 500);

            return new ParsedMatchScore(totalScore, matchReason);
        } catch (Exception e) {
            log.warn("Failed to parse AI match score: {}", e.getMessage());
            return new ParsedMatchScore(60, "基于语义相似度匹配");
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

    private int clampScore(int score) {
        if (score < 0) {
            return 0;
        }
        return Math.min(score, 100);
    }

    private int calculateTotalScore(int summary, int education, int experience, int skill, int project) {
        double total = summary * 0.10 + education * 0.15 + experience * 0.25 + skill * 0.25 + project * 0.25;
        return (int) Math.round(total);
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

    private record JobScore(TargetJob job, double similarity) {
    }

    private record ParsedMatchScore(int matchScore, String matchReason) {
    }
}