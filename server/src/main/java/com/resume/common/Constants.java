package com.resume.common;

public final class Constants {
    private Constants() {}

    public static final class InterviewStatus {
        public static final String ACTIVE = "ACTIVE";
        public static final String COMPLETED = "COMPLETED";
        private InterviewStatus() {}
    }

    public static final class MessageRole {
        public static final String USER = "user";
        public static final String ASSISTANT = "assistant";
        private MessageRole() {}
    }

    public static final class AiProvider {
        public static final String OPENAI = "openai";
        public static final String QWEN = "qwen";
        private AiProvider() {}
    }
}
