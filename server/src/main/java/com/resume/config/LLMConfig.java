package com.resume.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.ai")
public class LLMConfig {
    private String provider = "openai";
    private OpenAi openai = new OpenAi();
    private Qwen qwen = new Qwen();

    @Data
    public static class OpenAi {
        private String baseUrl;
        private String apiKey;
        private String model;
    }

    @Data
    public static class Qwen {
        private String baseUrl;
        private String apiKey;
        private String model;
    }
}