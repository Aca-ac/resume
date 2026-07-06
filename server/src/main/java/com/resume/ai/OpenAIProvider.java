package com.resume.ai;

import com.resume.common.Constants;
import com.resume.config.LLMConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAIProvider extends AbstractCompatibleLLMProvider {

    private final LLMConfig llmConfig;

    @Override
    public String name() {
        return Constants.AiProvider.OPENAI;
    }

    @Override
    protected String apiKey() {
        return llmConfig.getOpenai().getApiKey();
    }

    @Override
    protected String baseUrl() {
        return llmConfig.getOpenai().getBaseUrl();
    }

    @Override
    protected String model() {
        return llmConfig.getOpenai().getModel();
    }

    @Override
    protected String offlinePrefix() {
        return "[offline-ai] ";
    }
}
