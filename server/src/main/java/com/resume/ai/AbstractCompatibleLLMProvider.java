package com.resume.ai;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public abstract class AbstractCompatibleLLMProvider implements LLMProvider {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    protected abstract String apiKey();

    protected abstract String baseUrl();

    protected abstract String model();

    protected abstract String offlinePrefix();

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        if (apiKey() == null || apiKey().isBlank()) {
            return offline(userPrompt);
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        RestClient client = RestClient.builder()
                .baseUrl(baseUrl())
                .requestFactory(requestFactory)
                .build();
        Map<String, Object> body = Map.of(
                "model", model(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey())
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                return offline(userPrompt);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return String.valueOf(message.get("content"));
        } catch (Exception ex) {
            return offline(userPrompt);
        }
    }

    private String offline(String userPrompt) {
        int end = Math.min(200, userPrompt.length());
        return offlinePrefix() + userPrompt.substring(0, end);
    }
}