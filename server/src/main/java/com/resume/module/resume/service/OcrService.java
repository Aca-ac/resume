package com.resume.module.resume.service;

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

@Service
public class OcrService {

    private final RestClient restClient = RestClient.create();

    @Value("${app.ai.qwen.api-key:}")
    private String apiKey;

    @Value("${app.ai.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    public String recognize(Path imagePath, String fileType) {
        if (apiKey == null || apiKey.isBlank()) {
            return "[OCR占位结果] 请配置 DASHSCOPE_API_KEY 后重新识别。文件: " + imagePath.getFileName();
        }
        try {
            byte[] bytes = Files.readAllBytes(imagePath);
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String mime = fileType.equalsIgnoreCase("PNG") ? "image/png" : "image/jpeg";
            String dataUrl = "data:" + mime + ";base64," + base64;

            Map<String, Object> body = Map.of(
                    "model", "qwen-vl-plus",
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of("type", "image_url", "image_url", Map.of("url", dataUrl)),
                                    Map.of("type", "text", "text", "请识别图片中的简历文字，按原文输出")
                            )
                    ))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .headers(h -> {
                        h.addAll(headers);
                    })
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (resp == null) {
                return "[OCR失败] 空响应";
            }
            Object choices = resp.get("choices");
            if (choices instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map<?, ?> choice) {
                    Object message = choice.get("message");
                    if (message instanceof Map<?, ?> msg) {
                        Object content = msg.get("content");
                        if (content != null) {
                            return content.toString();
                        }
                    }
                }
            }
            return "[OCR失败] 无法解析响应";
        } catch (Exception e) {
            return "[OCR失败] " + e.getMessage();
        }
    }
}
