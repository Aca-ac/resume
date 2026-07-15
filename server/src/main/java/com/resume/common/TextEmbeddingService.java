package com.resume.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.client.MultipartBodyBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextEmbeddingService {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper;

    private static final TypeReference<List<Double>> DOUBLE_LIST_TYPE = new TypeReference<>() {};
    private static final int MAX_TOKENS = 8192;
    private static final int TOKEN_MULTIPLIER = 4;
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(5);
    private static final Duration MAX_WAIT_TIME = Duration.ofMinutes(5);

    @Value("${app.ai.qwen.api-key:}")
    private String apiKey;

    @Value("${app.ai.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${app.ai.qwen.embedding-model:text-embedding-v4}")
    private String embeddingModel;

    public List<Double> embed(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(400, "文本内容不能为空");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(500, "未配置 DASHSCOPE_API_KEY，无法生成文本向量");
        }

        String truncatedText = truncateText(text);

        Map<String, Object> body = Map.of(
                "model", embeddingModel,
                "input", truncatedText,
                "encoding_format", "float"
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restClient.post()
                    .uri(baseUrl + "/embeddings")
                    .headers(h -> h.addAll(headers))
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return parseEmbeddingResponse(resp);
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("Embedding API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(500, "向量生成服务异常：" + e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("Embedding failed: {}", e.getMessage());
            throw new BusinessException(500, "向量生成失败：" + e.getMessage());
        }
    }

    public String embedToJson(String text) {
        List<Double> vector = embed(text);
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            log.warn("Failed to serialize embedding to JSON: {}", e.getMessage());
            throw new BusinessException(500, "向量序列化失败");
        }
    }

    public List<List<Double>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new BusinessException(400, "文本列表不能为空");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(500, "未配置 DASHSCOPE_API_KEY，无法生成文本向量");
        }

        List<String> truncatedTexts = texts.stream()
                .map(this::truncateText)
                .toList();

        String inputJsonl = buildBatchInputJsonl(truncatedTexts);

        try {
            String inputFileId = uploadBatchInputFile(inputJsonl);
            log.info("Batch input file uploaded, file_id: {}", inputFileId);

            String batchId = submitBatchJob(inputFileId);
            log.info("Batch job submitted, batch_id: {}", batchId);

            Map<String, Object> result = pollBatchJob(batchId);

            if ("failed".equals(result.get("status"))) {
                Object error = result.get("error");
                log.error("Batch job failed: {}", error);
                throw new BusinessException(500, "批量向量生成任务失败");
            }

            String outputFileId = (String) result.get("output_file_id");
            String resultsJsonl = downloadBatchResult(outputFileId);

            return parseBatchResults(resultsJsonl, texts.size());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Batch embedding failed: {}", e.getMessage());
            throw new BusinessException(500, "批量向量生成失败：" + e.getMessage());
        }
    }

    public List<String> embedBatchToJson(List<String> texts) {
        List<List<Double>> vectors = embedBatch(texts);
        return vectors.stream()
                .map(vector -> {
                    try {
                        return objectMapper.writeValueAsString(vector);
                    } catch (Exception e) {
                        log.warn("Failed to serialize embedding to JSON: {}", e.getMessage());
                        throw new BusinessException(500, "向量序列化失败");
                    }
                })
                .toList();
    }

    public List<Double> parseVectorJson(String json) {
        if (json == null || json.isBlank()) {
            throw new BusinessException(400, "向量JSON不能为空");
        }
        try {
            return objectMapper.readValue(json, DOUBLE_LIST_TYPE);
        } catch (Exception e) {
            log.warn("Failed to parse embedding JSON: {}", e.getMessage());
            throw new BusinessException(400, "向量格式无效");
        }
    }

    public double calculateCosineSimilarity(List<Double> vector1, List<Double> vector2) {
        if (vector1 == null || vector2 == null || vector1.isEmpty() || vector2.isEmpty()) {
            return 0.0;
        }
        if (vector1.size() != vector2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += vector1.get(i) * vector1.get(i);
            norm2 += vector2.get(i) * vector2.get(i);
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private String truncateText(String text) {
        if (text == null) {
            return "";
        }
        int maxLength = MAX_TOKENS * TOKEN_MULTIPLIER;
        if (text.length() <= maxLength) {
            return text.trim();
        }
        log.warn("Text exceeds max token limit, truncating from {} to {} characters", text.length(), maxLength);
        return text.substring(0, maxLength).trim();
    }

    private String buildBatchInputJsonl(List<String> texts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            try {
                Map<String, Object> request = Map.of(
                        "custom_id", "embedding_" + UUID.randomUUID().toString().substring(0, 8) + "_" + i,
                        "method", "POST",
                        "url", "/v1/embeddings",
                        "body", Map.of(
                                "model", embeddingModel,
                                "input", texts.get(i),
                                "encoding_format", "float"
                        )
                );
                sb.append(objectMapper.writeValueAsString(request)).append("\n");
            } catch (Exception e) {
                log.warn("Failed to build batch input line {}: {}", i, e.getMessage());
            }
        }
        return sb.toString();
    }

    private String uploadBatchInputFile(String inputJsonl) {
        try {
            byte[] content = inputJsonl.getBytes(StandardCharsets.UTF_8);
            Resource resource = new ByteArrayResource(content) {
                @Override
                public String getFilename() {
                    return "batch_embedding_input.jsonl";
                }
            };

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", resource)
                    .contentType(MediaType.parseMediaType("application/jsonl"));
            builder.part("purpose", "batch");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restClient.post()
                    .uri(baseUrl + "/files")
                    .headers(h -> h.addAll(headers))
                    .body(builder.build())
                    .retrieve()
                    .body(Map.class);

            if (resp == null || !resp.containsKey("id")) {
                throw new BusinessException(500, "文件上传失败，未返回file_id");
            }

            return (String) resp.get("id");
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("File upload API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(500, "文件上传失败：" + e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("File upload failed: {}", e.getMessage());
            throw new BusinessException(500, "文件上传失败：" + e.getMessage());
        }
    }

    private String submitBatchJob(String inputFileId) {
        try {
            Map<String, Object> body = Map.of(
                    "input_file_id", inputFileId,
                    "endpoint", "/v1/embeddings",
                    "completion_window", "24h"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restClient.post()
                    .uri(baseUrl + "/batches")
                    .headers(h -> h.addAll(headers))
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (resp == null || !resp.containsKey("id")) {
                throw new BusinessException(500, "批量任务提交失败，未返回batch_id");
            }

            return (String) resp.get("id");
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("Batch submit API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(500, "批量任务提交失败：" + e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("Batch submit failed: {}", e.getMessage());
            throw new BusinessException(500, "批量任务提交失败：" + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> pollBatchJob(String batchId) throws InterruptedException {
        LocalDateTime startTime = LocalDateTime.now();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        while (true) {
            Duration elapsed = Duration.between(startTime, LocalDateTime.now());
            if (elapsed.compareTo(MAX_WAIT_TIME) > 0) {
                throw new BusinessException(500, "批量任务超时，等待时间超过" + MAX_WAIT_TIME.toMinutes() + "分钟");
            }

            try {
                Map<String, Object> resp = restClient.get()
                        .uri(baseUrl + "/batches/" + batchId)
                        .headers(h -> h.addAll(headers))
                        .retrieve()
                        .body(Map.class);

                if (resp == null) {
                    throw new BusinessException(500, "批量任务状态查询失败");
                }

                String status = (String) resp.get("status");
                log.info("Batch job status: {}, elapsed: {}s", status, elapsed.getSeconds());

                if ("completed".equals(status) || "failed".equals(status)) {
                    return resp;
                }

                Thread.sleep(POLL_INTERVAL.toMillis());
            } catch (RestClientResponseException e) {
                log.warn("Batch status API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
                Thread.sleep(POLL_INTERVAL.toMillis());
            }
        }
    }

    private String downloadBatchResult(String fileId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);

            return restClient.get()
                    .uri(baseUrl + "/files/" + fileId + "/content")
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.warn("Batch result download error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(500, "批量结果下载失败：" + e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("Batch result download failed: {}", e.getMessage());
            throw new BusinessException(500, "批量结果下载失败：" + e.getMessage());
        }
    }

    private List<List<Double>> parseBatchResults(String resultsJsonl, int expectedSize) {
        if (resultsJsonl == null || resultsJsonl.isBlank()) {
            throw new BusinessException(500, "批量结果为空");
        }

        List<Map<?, ?>> resultItems = new ArrayList<>();
        String[] lines = resultsJsonl.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                @SuppressWarnings("unchecked")
                Map<?, ?> item = objectMapper.readValue(line, Map.class);

                if (item.containsKey("error")) {
                    Object error = item.get("error");
                    String customId = (String) item.get("custom_id");
                    String errorMsg = error instanceof Map<?, ?> errorMap
                            ? (String) errorMap.get("message")
                            : error.toString();
                    log.error("Batch item {} failed: {}", customId, errorMsg);
                    throw new BusinessException(500, "批量任务部分失败，custom_id=" + customId + ", error=" + errorMsg);
                }

                resultItems.add(item);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Failed to parse batch result line: {}", e.getMessage());
            }
        }

        List<Map<?, ?>> sortedItems = resultItems.stream()
                .filter(item -> item.containsKey("response"))
                .sorted(Comparator.comparingInt(item -> {
                    Object customIdObj = item.get("custom_id");
                    if (customIdObj instanceof String customId) {
                        int lastUnderscore = customId.lastIndexOf('_');
                        if (lastUnderscore >= 0 && lastUnderscore < customId.length() - 1) {
                            try {
                                return Integer.parseInt(customId.substring(lastUnderscore + 1));
                            } catch (NumberFormatException e) {
                                log.warn("Failed to parse index from custom_id: {}", customId);
                            }
                        }
                    }
                    return 0;
                }))
                .collect(Collectors.toList());

        List<List<Double>> vectors = new ArrayList<>(expectedSize);
        for (int i = 0; i < expectedSize; i++) {
            vectors.add(List.of());
        }

        for (Map<?, ?> item : sortedItems) {
            Object response = item.get("response");
            if (!(response instanceof Map<?, ?> respMap)) {
                continue;
            }

            Object body = respMap.get("body");
            if (!(body instanceof Map<?, ?> bodyMap)) {
                continue;
            }

            Object data = bodyMap.get("data");
            if (!(data instanceof List<?> dataList) || dataList.isEmpty()) {
                continue;
            }

            Object first = dataList.get(0);
            if (!(first instanceof Map<?, ?> firstMap)) {
                continue;
            }

            Object indexObj = firstMap.get("index");
            int index = indexObj instanceof Number ? ((Number) indexObj).intValue() : 0;

            Object embedding = firstMap.get("embedding");
            if (!(embedding instanceof List<?> embeddingList)) {
                log.warn("Unexpected batch embedding format: embedding is not a list at index {}", index);
                continue;
            }

            List<Double> vector = embeddingList.stream()
                    .map(val -> {
                        if (val instanceof Number number) {
                            return number.doubleValue();
                        }
                        return 0.0;
                    })
                    .toList();

            if (index >= 0 && index < expectedSize) {
                vectors.set(index, vector);
            }
        }

        return vectors;
    }

    private List<Double> parseEmbeddingResponse(Map<String, Object> resp) {
        if (resp == null) {
            throw new BusinessException(500, "向量生成API返回空响应");
        }

        Object data = resp.get("data");
        if (!(data instanceof List<?> dataList) || dataList.isEmpty()) {
            log.warn("Unexpected embedding response format: data is not a list or is empty");
            throw new BusinessException(500, "向量生成API返回格式错误");
        }

        Object firstItem = dataList.get(0);
        if (!(firstItem instanceof Map<?, ?> itemMap)) {
            log.warn("Unexpected embedding response format: first item is not a map");
            throw new BusinessException(500, "向量生成API返回格式错误");
        }

        Object embedding = itemMap.get("embedding");
        if (!(embedding instanceof List<?> embeddingList)) {
            log.warn("Unexpected embedding response format: embedding is not a list");
            throw new BusinessException(500, "向量生成API返回格式错误");
        }

        return embeddingList.stream()
                .map(item -> {
                    if (item instanceof Number number) {
                        return number.doubleValue();
                    }
                    return 0.0;
                })
                .toList();
    }
}