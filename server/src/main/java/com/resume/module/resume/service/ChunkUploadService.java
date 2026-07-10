package com.resume.module.resume.service;

import com.resume.config.StorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class ChunkUploadService {

    private final Path chunkRoot;
    private final Path fileRoot;

    public ChunkUploadService(StorageProperties properties) throws IOException {
        this.chunkRoot = Path.of(properties.getChunkDir()).toAbsolutePath().normalize();
        this.fileRoot = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(chunkRoot);
        Files.createDirectories(fileRoot);
    }

    public void saveChunk(String uploadId, int chunkIndex, MultipartFile chunk) throws IOException {
        Path dir = chunkRoot.resolve(uploadId);
        Files.createDirectories(dir);
        Files.write(dir.resolve("part-" + chunkIndex), chunk.getBytes());
    }

    public FileStorageService.StoredFile merge(String uploadId, String originalName, int totalChunks) throws IOException {
        Path dir = chunkRoot.resolve(uploadId);
        String ext = ext(originalName);
        String storedName = UUID.randomUUID() + ext;
        Path target = fileRoot.resolve(storedName);

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                for (int i = 0; i < totalChunks; i++) {
                    Path part = dir.resolve("part-" + i);
                    if (!Files.exists(part)) {
                        throw new IllegalArgumentException("缺少分片: " + i);
                    }
                    byte[] bytes = Files.readAllBytes(part);
                    md.update(bytes);
                    out.write(bytes);
                }
            }
            deleteDir(dir);
            return new FileStorageService.StoredFile(storedName, originalName, target, Files.size(target),
                    HexFormat.of().formatHex(md.digest()));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String md5(MultipartFile file) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        try (InputStream in = file.getInputStream(); DigestInputStream din = new DigestInputStream(in, md)) {
            din.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(md.digest());
    }

    private static String ext(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }

    private static void deleteDir(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        }
    }
}
