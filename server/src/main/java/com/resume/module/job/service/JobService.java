package com.resume.module.job.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.BusinessException;
import com.resume.module.job.dto.CommentCreateRequest;
import com.resume.module.job.dto.CommentVO;
import com.resume.module.job.dto.JobCreateRequest;
import com.resume.module.job.dto.JobSearchRequest;
import com.resume.module.job.dto.JobSearchResultVO;
import com.resume.module.job.dto.JobSimpleVO;
import com.resume.module.job.dto.JobUpdateRequest;
import com.resume.module.job.dto.CommunityJobVO;
import com.resume.module.job.dto.JobVO;
import com.resume.module.job.dto.OwnerVO;
import com.resume.module.job.dto.PageResult;
import com.resume.module.job.entity.JobComment;
import com.resume.module.job.entity.TargetJob;
import com.resume.module.job.mapper.JobCommentMapper;
import com.resume.module.job.mapper.TargetJobMapper;
import com.resume.user_identify.entity.User;
import com.resume.user_identify.mapper.UserMapper;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Integer SOURCE_MANUAL = 0;
    private static final Integer SOURCE_AI = 1;
    private static final Integer SOURCE_FORK = 2;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final TargetJobMapper targetJobMapper;
    private final JobCommentMapper jobCommentMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final RestClient restClient = RestClient.create();

    @Value("${app.ai.doubao.api-key:}")
    private String apiKey;

    @Value("${app.ai.doubao.base-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String baseUrl;

    @Value("${app.ai.doubao.model:doubao-seed-2-1-pro-260628}")
    private String model;

    @Transactional
    public JobVO createJob(Long userId, JobCreateRequest request) {
        if (request.getJobName() == null || request.getJobName().isBlank()) {
            throw new BusinessException(400, "岗位名称不能为空");
        }

        TargetJob job = new TargetJob();
        job.setUserId(userId);
        job.setJobName(request.getJobName().trim());
        job.setJdContent(request.getJdContent());
        job.setSource(SOURCE_MANUAL);

        targetJobMapper.insert(job);

        return toVO(job);
    }

    public PageResult<JobSimpleVO> listJobs(Long userId, int page, int size) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 20;
        }
        Page<TargetJob> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TargetJob> wrapper = new LambdaQueryWrapper<TargetJob>()
                .eq(TargetJob::getUserId, userId)
                .orderByDesc(TargetJob::getCreatedAt);
        Page<TargetJob> result = targetJobMapper.selectPage(pageParam, wrapper);
        List<JobSimpleVO> content = result.getRecords().stream()
                .map(this::toSimpleVO)
                .toList();
        return new PageResult<>(content, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public JobVO getJob(Long userId, Long jobId) {
        TargetJob job = targetJobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(404, "岗位不存在");
        }

        JobVO vo = toVO(job);

        User user = userMapper.selectById(job.getUserId());
        if (user != null) {
            OwnerVO owner = new OwnerVO();
            owner.setId(user.getId());
            owner.setNickname(user.getNickname());
            vo.setOwner(owner);
        }

        return vo;
    }

    @Transactional
    public JobVO updateJob(Long userId, Long jobId, JobUpdateRequest request) {
        TargetJob job = targetJobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(404, "岗位不存在");
        }

        if (!job.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权修改该岗位");
        }

        if (request.getJobName() != null && !request.getJobName().isBlank()) {
            job.setJobName(request.getJobName().trim());
        }
        if (request.getJdContent() != null) {
            job.setJdContent(request.getJdContent());
        }

        targetJobMapper.updateById(job);

        return toVO(job);
    }

    @Transactional
    public void deleteJob(Long userId, Long jobId) {
        TargetJob job = targetJobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(404, "岗位不存在");
        }

        if (!job.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除该岗位");
        }

        targetJobMapper.deleteById(jobId);
    }

    @Transactional
    public JobSearchResultVO searchJob(Long userId, JobSearchRequest request) {
        if (request.getJobName() == null || request.getJobName().isBlank()) {
            throw new BusinessException(400, "岗位名称不能为空");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(500, "未配置 ARK_API_KEY，无法进行 AI 搜索");
        }

        String promptTemplate = loadSearchPromptTemplate();
        String prompt = promptTemplate
                .replace("【岗位名称】", request.getJobName().trim())
                .replace("【已有JD内容】", request.getExistingJd() == null ? "" : request.getExistingJd().trim());

        try {
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

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restClient.post()
                    .uri(baseUrl + "/responses")
                    .headers(h -> h.addAll(headers))
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            log.info("Job search API response: {}", resp);

            String content = parseResponseContent(resp);
            if (content == null || content.isBlank()) {
                throw new BusinessException(500, "AI搜索失败：API返回内容为空");
            }

            JobSearchResultVO searchResult = parseSearchResult(content.trim());
            if (searchResult.getJdContent() == null || searchResult.getJdContent().isBlank()) {
                throw new BusinessException(500, "AI搜索失败，请稍后重试");
            }

            if (Boolean.FALSE.equals(searchResult.getWebSearchSuccess())) {
                throw new BusinessException(500, "联网搜索失败：" + searchResult.getJdContent());
            }

            TargetJob job = new TargetJob();
            job.setUserId(userId);
            job.setJobName(searchResult.getJobName() != null ? searchResult.getJobName().trim() : request.getJobName().trim());
            job.setJdContent(searchResult.getJdContent());
            job.setSource(SOURCE_AI);
            if (searchResult.getSources() != null && !searchResult.getSources().isEmpty()) {
                job.setSourceUrls(objectMapper.writeValueAsString(searchResult.getSources()));
            }

            targetJobMapper.insert(job);

            return searchResult;
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("Job search API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(500, "AI搜索服务异常：" + e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("Job search failed: {}", e.getMessage());
            throw new BusinessException(500, "AI搜索失败：" + e.getMessage());
        }
    }

    public PageResult<CommunityJobVO> listCommunity(int page, int size) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 20;
        }
        Page<TargetJob> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TargetJob> wrapper = new LambdaQueryWrapper<TargetJob>()
                .orderByDesc(TargetJob::getCreatedAt);
        Page<TargetJob> result = targetJobMapper.selectPage(pageParam, wrapper);

        List<TargetJob> records = result.getRecords();
        Map<Long, User> userMap = batchGetUsers(records.stream()
                .map(TargetJob::getUserId)
                .distinct()
                .toList());

        List<CommunityJobVO> content = records.stream()
                .map(job -> toCommunityVO(job, userMap))
                .toList();

        return new PageResult<>(content, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public PageResult<CommunityJobVO> searchCommunity(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(400, "岗位名称不能为空");
        }
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 20;
        }
        Page<TargetJob> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TargetJob> wrapper = new LambdaQueryWrapper<TargetJob>()
                .like(TargetJob::getJobName, keyword.trim())
                .orderByDesc(TargetJob::getCreatedAt);
        Page<TargetJob> result = targetJobMapper.selectPage(pageParam, wrapper);

        List<TargetJob> records = result.getRecords();
        Map<Long, User> userMap = batchGetUsers(records.stream()
                .map(TargetJob::getUserId)
                .distinct()
                .toList());

        List<CommunityJobVO> content = records.stream()
                .map(job -> toCommunityVO(job, userMap))
                .toList();

        return new PageResult<>(content, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public JobVO forkJob(Long userId, Long jobId) {
        TargetJob sourceJob = targetJobMapper.selectById(jobId);
        if (sourceJob == null) {
            throw new BusinessException(404, "岗位不存在");
        }

        if (sourceJob.getUserId().equals(userId)) {
            throw new BusinessException(400, "不能fork自己的岗位");
        }

        TargetJob newJob = new TargetJob();
        newJob.setUserId(userId);
        newJob.setJobName(sourceJob.getJobName());
        newJob.setJdContent(sourceJob.getJdContent());
        newJob.setSource(SOURCE_FORK);
        newJob.setOriginalJobId(jobId);
        newJob.setSourceUrls(sourceJob.getSourceUrls());

        targetJobMapper.insert(newJob);

        return toVO(newJob);
    }

    public PageResult<CommentVO> listComments(Long userId, Long jobId, int page, int size) {
        TargetJob job = targetJobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(404, "岗位不存在");
        }

        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 20;
        }
        Page<JobComment> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<JobComment> wrapper = new LambdaQueryWrapper<JobComment>()
                .eq(JobComment::getJobId, jobId)
                .orderByDesc(JobComment::getCreatedAt);
        Page<JobComment> result = jobCommentMapper.selectPage(pageParam, wrapper);

        List<JobComment> records = result.getRecords();
        Map<Long, User> userMap = batchGetUsers(records.stream()
                .map(JobComment::getUserId)
                .distinct()
                .toList());

        List<CommentVO> content = records.stream()
                .map(comment -> toCommentVO(comment, userMap))
                .toList();

        return new PageResult<>(content, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public CommentVO createComment(Long userId, Long jobId, CommentCreateRequest request) {
        TargetJob job = targetJobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(404, "岗位不存在");
        }

        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BusinessException(400, "评论内容不能为空");
        }

        String content = request.getContent().trim();
        if (content.length() > 5000) {
            throw new BusinessException(400, "评论内容过长");
        }

        JobComment comment = new JobComment();
        comment.setJobId(jobId);
        comment.setUserId(userId);
        comment.setContent(content);

        jobCommentMapper.insert(comment);

        Map<Long, User> userMap = batchGetUsers(List.of(userId));
        return toCommentVO(comment, userMap);
    }

    private CommentVO toCommentVO(JobComment comment, Map<Long, User> userMap) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setJobId(comment.getJobId());
        vo.setContent(comment.getContent());
        vo.setCreatedAt(comment.getCreatedAt() == null ? null : comment.getCreatedAt().format(FMT));

        User user = userMap.get(comment.getUserId());
        if (user != null) {
            OwnerVO owner = new OwnerVO();
            owner.setId(user.getId());
            owner.setNickname(user.getNickname());
            vo.setUser(owner);
        }

        return vo;
    }

    private Map<Long, User> batchGetUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private CommunityJobVO toCommunityVO(TargetJob job, Map<Long, User> userMap) {
        CommunityJobVO vo = new CommunityJobVO();
        vo.setId(job.getId());
        vo.setJobName(job.getJobName());
        vo.setSource(job.getSource());
        vo.setCreatedAt(job.getCreatedAt() == null ? null : job.getCreatedAt().format(FMT));

        User user = userMap.get(job.getUserId());
        if (user != null) {
            OwnerVO owner = new OwnerVO();
            owner.setId(user.getId());
            owner.setNickname(user.getNickname());
            vo.setOwner(owner);
        }

        return vo;
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

    private String loadSearchPromptTemplate() {
        try {
            Resource resource = resourceLoader.getResource("classpath:prompts/job_search_prompt.txt");
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

    private JobSearchResultVO parseSearchResult(String raw) {
        String json = extractJsonObject(raw);
        try {
            return objectMapper.readValue(json, JobSearchResultVO.class);
        } catch (Exception e) {
            log.warn("Failed to parse search result: {}", e.getMessage());
            JobSearchResultVO fallback = new JobSearchResultVO();
            fallback.setJobName("");
            fallback.setJdContent(raw);
            fallback.setSources(List.of());
            return fallback;
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

    private List<String> parseSourceUrls(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }

    private JobVO toVO(TargetJob job) {
        JobVO vo = new JobVO();
        vo.setId(job.getId());
        vo.setJobName(job.getJobName());
        vo.setJdContent(job.getJdContent());
        vo.setSource(job.getSource());
        vo.setOriginalJobId(job.getOriginalJobId());
        vo.setSourceUrls(parseSourceUrls(job.getSourceUrls()));
        vo.setCreatedAt(job.getCreatedAt() == null ? null : job.getCreatedAt().format(FMT));
        vo.setUpdatedAt(job.getUpdatedAt() == null ? null : job.getUpdatedAt().format(FMT));
        return vo;
    }

    private JobSimpleVO toSimpleVO(TargetJob job) {
        JobSimpleVO vo = new JobSimpleVO();
        vo.setId(job.getId());
        vo.setJobName(job.getJobName());
        vo.setSource(job.getSource());
        vo.setSourceUrls(parseSourceUrls(job.getSourceUrls()));
        vo.setCreatedAt(job.getCreatedAt() == null ? null : job.getCreatedAt().format(FMT));
        return vo;
    }
}