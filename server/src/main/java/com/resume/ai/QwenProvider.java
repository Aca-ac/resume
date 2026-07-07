package com.resume.ai;

import com.resume.common.Constants;
import com.resume.config.LLMConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QwenProvider extends AbstractCompatibleLLMProvider {

    private final LLMConfig llmConfig;

    @Override
    public String name() {
        return Constants.AiProvider.QWEN;
    }

    @Override
    protected String apiKey() {
        return llmConfig.getQwen().getApiKey();
    }

    @Override
    protected String baseUrl() {
        return llmConfig.getQwen().getBaseUrl();
    }

    @Override
    protected String model() {
        return llmConfig.getQwen().getModel();
    }
}