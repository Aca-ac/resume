package com.resume.module.resume.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OcrService {

    private static final String FALLBACK = "【图片识别暂不可用】请手动编辑简历内容，或稍后重试 OCR。";

    private final RestClient restClient = RestClient.create();

    @Value("${app.ai.qwen.api-key:}")
    private String apiKey;

    @Value("${dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${app.ai.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    public String recognize(Path imagePath, String fileType) {
        try {
            if (imagePath == null || !Files.exists(imagePath)) {
                return FALLBACK;
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
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String mime = "PNG".equalsIgnoreCase(fileType) ? "image/png" : "image/jpeg";
            String dataUrl = "data:" + mime + ";base64," + base64;

            Map<String, Object> body = Map.of(
                    "model", "qwen-vl-plus",
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of("type", "image_url", "image_url", Map.of("url", dataUrl)),
                                    Map.of("type", "text", "text", "识别图片中的简历文字，按原文输出，保持段落结构")
                            )
                    ))
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
                return FALLBACK;
            }
            return parsed;
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
        return content == null ? null : content.toString();
    }
}
