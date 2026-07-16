package com.resume.module.resume.service;

import com.resume.config.VoiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceStorageServiceTest {

    @TempDir
    Path tempDir;

    private VoiceStorageService storageService;

    @BeforeEach
    void setUp() {
        VoiceProperties props = new VoiceProperties();
        props.setUploadDir(tempDir.toString());
        props.setMaxFileSize(1024 * 1024);
        storageService = new VoiceStorageService(props);
    }

    @Test
    void store_savesFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.wav", "audio/wav", new byte[]{1, 2, 3});
        VoiceStorageService.StoredVoice stored = storageService.store(file);
        assertEquals("test.wav", stored.originalName());
        assertTrue(stored.path().toFile().exists());
    }
}
