package com.resume.module.resume.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OcrService {

    private static final String FALLBACK = "【图片识别暂不可用】请手动编辑简历内容，或稍后重试 OCR。";
    private static final int MIN_PIXELS = 32 * 32 * 3;
    private static final int MAX_PIXELS = 32 * 32 * 8192;

    private final RestClient restClient = RestClient.create();

    @Value("${app.ai.qwen.api-key:}")
    private String apiKey;

    @Value("${dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${app.ai.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${app.ai.qwen.ocr-model:qwen-vl-ocr}")
    private String ocrModel;

    public String recognize(Path imagePath, String fileType) {
        try {
            if (imagePath == null || !Files.exists(imagePath)) {
                log.warn("OCR skipped: file not found at {}", imagePath);
                return FALLBACK + "（图片文件不存在）";
            }
            String key = resolveApiKey();
            if (key.isBlank()) {
                return FALLBACK + "（未配置 DASHSCOPE_API_KEY）";
            }
            long size = Files.size(imagePath);
            if (size > 20 * 1024 * 1024) {
                return FALLBACK + "（图片过大，建议压缩后重试）";
            }

            byte[] bytes = Files.readAllBytes(imagePath);
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
            String mime = "PNG".equalsIgnoreCase(fileType) ? "image/png" : "image/jpeg";
            String dataUrl = "data:" + mime + ";base64," + base64;

            Map<String, Object> imagePart = new LinkedHashMap<>();
            imagePart.put("type", "image_url");
            imagePart.put("image_url", Map.of("url", dataUrl));
            imagePart.put("min_pixels", MIN_PIXELS);
            imagePart.put("max_pixels", MAX_PIXELS);

            List<Map<String, Object>> content = new ArrayList<>();
            content.add(imagePart);
            content.add(Map.of(
                    "type", "text",
                    "text", "请识别图片中的简历文字，按原文逐行输出，保持段落结构，不要添加解释"
            ));

            Map<String, Object> body = Map.of(
                    "model", ocrModel,
                    "messages", List.of(Map.of("role", "user", "content", content))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(key);

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .headers(h -> h.addAll(headers))
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String parsed = parseContent(resp);
            if (parsed == null || parsed.isBlank()) {
                log.warn("OCR empty response from model {}", ocrModel);
                return FALLBACK;
            }
            log.info("OCR success: {} chars from {}", parsed.length(), imagePath.getFileName());
            return parsed;
        } catch (RestClientResponseException e) {
            log.warn("OCR API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return FALLBACK + "（识别服务异常：" + e.getStatusCode().value() + "）";
        } catch (Exception e) {
            log.warn("OCR fallback: {}", e.getMessage());
            return FALLBACK;
        }
    }

    private String resolveApiKey() {
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey;
        }
        return dashscopeApiKey == null ? "" : dashscopeApiKey;
    }

    private String parseContent(Map<String, Object> resp) {
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
        return content == null ? null : content.toString().trim();
    }
}
