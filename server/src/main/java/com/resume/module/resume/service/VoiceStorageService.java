package com.resume.module.resume.service;

import com.resume.common.BusinessException;
import com.resume.config.VoiceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoiceStorageService {

    private final VoiceProperties properties;
    private Path root;

    private Path root() throws IOException {
        if (root == null) {
            root = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(root);
        }
        return root;
    }

    public StoredVoice store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请上传音频文件");
        }
        if (file.getSize() > properties.getMaxFileSize()) {
            throw new BusinessException(400, "音频文件过大，最大 " + (properties.getMaxFileSize() / 1024 / 1024) + "MB");
        }
        cleanupExpired();
        String original = file.getOriginalFilename() == null ? "audio.wav" : file.getOriginalFilename();
        String ext = extension(original);
        String storedName = UUID.randomUUID() + ext;
        Path target = root().resolve(storedName);
        file.transferTo(target);
        return new StoredVoice(storedName, original, target, ext);
    }

    public Path resolve(String storedName) {
        try {
            Path resolved = root().resolve(storedName).normalize();
            if (!resolved.startsWith(root())) {
                throw new BusinessException(400, "非法文件路径");
            }
            return resolved;
        } catch (IOException e) {
            throw new BusinessException(500, "语音存储目录不可用");
        }
    }

    public byte[] readBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    public Path storeBytes(byte[] data, String ext) throws IOException {
        cleanupExpired();
        String storedName = UUID.randomUUID() + (ext.startsWith(".") ? ext : "." + ext);
        Path target = root().resolve(storedName);
        Files.write(target, data);
        return target;
    }

    private void cleanupExpired() throws IOException {
        Path base = root();
        if (!Files.isDirectory(base)) {
            return;
        }
        Instant cutoff = Instant.now().minus(properties.getFileTtlHours(), ChronoUnit.HOURS);
        try (var stream = Files.list(base)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toInstant().isBefore(cutoff);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    private static String extension(String original) {
        int dot = original.lastIndexOf('.');
        if (dot < 0) {
            return ".wav";
        }
        return original.substring(dot).toLowerCase();
    }

    public record StoredVoice(String storedName, String originalName, Path path, String extension) {
    }
}
