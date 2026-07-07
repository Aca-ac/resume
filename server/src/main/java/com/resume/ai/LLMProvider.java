package com.resume.ai;

public interface LLMProvider {
    String name();

    String chat(String systemPrompt, String userPrompt);
}
