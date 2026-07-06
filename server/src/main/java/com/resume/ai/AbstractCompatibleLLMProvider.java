package com.resume.ai;

import com.resume.common.BusinessException;
import com.resume.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public abstract class AbstractCompatibleLLMProvider implements LLMProvider {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    private final Logger log = LoggerFactory.getLogger(getClass());
    private volatile RestClient restClient;

    protected abstract String apiKey();

    protected abstract String baseUrl();

    protected abstract String model();

    private RestClient client() {
        RestClient local = restClient;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (restClient == null) {
                String url = baseUrl();
                if (url == null || url.isBlank()) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM base URL is not configured");
                }
                SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
                requestFactory.setReadTimeout(READ_TIMEOUT);
                restClient = RestClient.builder()
                        .baseUrl(url)
                        .requestFactory(requestFactory)
                        .build();
            }
            return restClient;
        }
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        String key = apiKey();
        if (key == null || key.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM API key is not configured");
        }
        String modelName = model();
        if (modelName == null || modelName.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM model is not configured");
        }

        Map<String, Object> body = Map.of(
                "model", modelName,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = client().post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + key)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return extractContent(response);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("LLM HTTP request failed provider={}", name(), ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM request failed: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("LLM call failed provider={}", name(), ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM call failed: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM returned empty response");
        }
        Object choicesObj = response.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM response missing choices");
        }
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> choiceMap)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM response has invalid choice format");
        }
        Object messageObj = choiceMap.get("message");
        if (!(messageObj instanceof Map<?, ?> message)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM response missing message");
        }
        Object content = message.get("content");
        if (content == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM response missing content");
        }
        return content.toString();
    }
}