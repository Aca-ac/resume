package com.resume.module.resume.service;

import com.resume.common.BusinessException;
import com.resume.config.ExportProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
public class ExportStorageService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path root;
    private final int ttlHours;

    public ExportStorageService(ExportProperties properties) throws IOException {
        this.root = Paths.get(properties.getExportDir()).toAbsolutePath().normalize();
        this.ttlHours = Math.max(properties.getFileTtlHours(), 1);
        Files.createDirectories(root);
    }

    public StoredExport store(Long userId, Long resumeId, Long templateId, String format,
                              byte[] content, String downloadFilename) throws IOException {
        cleanupExpired();
        String exportId = UUID.randomUUID().toString().replace("-", "");
        String ext = "pdf".equals(format) ? ".pdf" : ".docx";
        Path target = root.resolve(exportId + "_" + userId + ext);
        Files.write(target, content);
        if (downloadFilename != null && !downloadFilename.isBlank()) {
            Files.writeString(root.resolve(exportId + "_" + userId + ".meta"), downloadFilename);
        }
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(ttlHours);
        log.info("Stored export file exportId={}, userId={}, format={}", exportId, userId, format);
        return new StoredExport(exportId, userId, resumeId, templateId, format, downloadFilename, target, expiresAt);
    }

    public StoredExport requireOwned(Long userId, String exportId) {
        try (Stream<Path> stream = Files.list(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith(exportId + "_" + userId)
                                && (name.endsWith(".pdf") || name.endsWith(".docx"));
                    })
                    .findFirst()
                    .map(path -> {
                        String fileName = path.getFileName().toString();
                        String format = fileName.endsWith(".pdf") ? "pdf" : "word";
                        String filename = readMetaFilename(exportId, userId);
                        return new StoredExport(exportId, userId, null, null, format, filename, path,
                                LocalDateTime.now().plusHours(ttlHours));
                    })
                    .orElseThrow(() -> new BusinessException(404, "导出文件不存在或已过期"));
        } catch (IOException e) {
            throw new BusinessException(500, "读取导出文件失败");
        }
    }

    public byte[] read(StoredExport stored) throws IOException {
        if (!Files.exists(stored.path())) {
            throw new BusinessException(404, "导出文件不存在或已过期");
        }
        return Files.readAllBytes(stored.path());
    }

    public String formatExpiresAt(LocalDateTime expiresAt) {
        return expiresAt == null ? null : expiresAt.format(FMT);
    }

    private String readMetaFilename(String exportId, Long userId) {
        Path meta = root.resolve(exportId + "_" + userId + ".meta");
        try {
            if (Files.exists(meta)) {
                return Files.readString(meta).trim();
            }
        } catch (IOException e) {
            log.debug("Read export meta failed: {}", e.getMessage());
        }
        return null;
    }

    private void cleanupExpired() {
        try (Stream<Path> stream = Files.list(root)) {
            LocalDateTime threshold = LocalDateTime.now().minusHours(ttlHours);
            stream.filter(Files::isRegularFile).forEach(path -> {
                try {
                    LocalDateTime modified = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault());
                    if (!modified.isBefore(threshold)) {
                        return;
                    }
                    String fileName = path.getFileName().toString();
                    Files.deleteIfExists(path);
                    if (fileName.endsWith(".meta")) {
                        return;
                    }
                    int underscore = fileName.indexOf('_');
                    if (underscore > 0) {
                        String prefix = fileName.substring(0, underscore);
                        int second = fileName.indexOf('_', underscore + 1);
                        String userPart = second > 0 ? fileName.substring(underscore + 1, second) : fileName.substring(underscore + 1);
                        if (userPart.matches("\\d+")) {
                            Files.deleteIfExists(root.resolve(prefix + "_" + userPart + ".meta"));
                        }
                    }
                } catch (IOException e) {
                    log.debug("Skip cleanup for {}: {}", path, e.getMessage());
                }
            });
        } catch (IOException e) {
            log.debug("Export cleanup skipped: {}", e.getMessage());
        }
    }

    public record StoredExport(
            String exportId,
            Long userId,
            Long resumeId,
            Long templateId,
            String format,
            String filename,
            Path path,
            LocalDateTime expiresAt
    ) {
    }
}
