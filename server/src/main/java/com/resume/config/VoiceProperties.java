package com.resume.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.voice")
public class VoiceProperties {

    /** 语音临时文件目录（相对或绝对路径） */
    private String uploadDir = "uploads/voice";

    /** 单文件最大字节数，默认 10MB */
    private long maxFileSize = 10 * 1024 * 1024;

    /** 临时文件保留小时数 */
    private int fileTtlHours = 24;

    /** Paraformer 实时识别模型 */
    private String asrModel = "paraformer-realtime-v2";

    /** 默认采样率（Hz） */
    private int asrSampleRate = 16000;

    /** CosyVoice 合成模型 */
    private String ttsModel = "cosyvoice-v3-flash";

    /** 默认音色 */
    private String ttsVoice = "longanyang";

    /** API 调用失败重试次数（不含首次） */
    private int maxRetries = 2;

    /** 重试间隔毫秒 */
    private long retryDelayMs = 500;
}
