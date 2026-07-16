package com.resume.module.resume.service;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.BusinessException;
import com.resume.config.VoiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechRecognitionService {

    private final VoiceProperties voiceProperties;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.qwen.api-key:}")
    private String apiKey;

    public String recognize(Path audioPath, String extension) {
        requireApiKey();
        String format = mapFormat(extension);
        Recognition recognizer = new Recognition();
        RecognitionParam param = RecognitionParam.builder()
                .apiKey(apiKey)
                .model(voiceProperties.getAsrModel())
                .format(format)
                .sampleRate(voiceProperties.getAsrSampleRate())
                .parameter("language_hints", new String[]{"zh", "en"})
                .build();
        try {
            String raw = recognizer.call(param, audioPath.toFile());
            return extractText(raw);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("ASR failed: {}", e.getMessage());
            throw new BusinessException(500, "语音识别失败：" + e.getMessage());
        } finally {
            try {
                recognizer.getDuplexApi().close(1000, "bye");
            } catch (Exception ignored) {
            }
        }
    }

    private String extractText(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(500, "语音识别结果为空");
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("{")) {
            return trimmed;
        }
        try {
            JsonNode root = objectMapper.readTree(trimmed);
            JsonNode text = root.path("text");
            if (!text.isMissingNode() && !text.asText().isBlank()) {
                return text.asText().trim();
            }
            JsonNode sentences = root.path("sentences");
            if (sentences.isArray() && !sentences.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode s : sentences) {
                    String t = s.path("text").asText("");
                    if (!t.isBlank()) {
                        if (!sb.isEmpty()) {
                            sb.append(' ');
                        }
                        sb.append(t.trim());
                    }
                }
                if (!sb.isEmpty()) {
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            log.debug("ASR JSON parse fallback: {}", e.getMessage());
        }
        return trimmed;
    }

    private String mapFormat(String extension) {
        String ext = extension == null ? "" : extension.toLowerCase(Locale.ROOT).replace(".", "");
        return switch (ext) {
            case "mp3" -> "mp3";
            case "pcm" -> "pcm";
            case "opus" -> "opus";
            case "webm" -> "webm";
            default -> "wav";
        };
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(500, "未配置 DASHSCOPE_API_KEY，无法进行语音识别");
        }
    }
}
