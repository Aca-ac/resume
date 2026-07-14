package com.resume.module.resume.service;

import com.resume.config.StorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(StorageProperties properties) throws IOException {
        this.root = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public StoredFile store(MultipartFile file) throws IOException {
        return store(file, null);
    }

    public StoredFile store(MultipartFile file, String subDir) throws IOException {
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = ext(original);
        String storedName = UUID.randomUUID() + ext;
        Path base = subDir == null || subDir.isBlank() ? root : root.resolve(subDir).normalize();
        if (!base.startsWith(root)) {
            throw new IllegalArgumentException("非法存储目录");
        }
        Files.createDirectories(base);
        Path target = base.resolve(storedName);
        String md5 = copyWithMd5(file.getInputStream(), target);
        String relative = subDir == null || subDir.isBlank() ? storedName : subDir.replace('\\', '/') + "/" + storedName;
        return new StoredFile(relative, original, target, file.getSize(), md5);
    }

    public StoredFile store(StoredFile existing) {
        return existing;
    }

    public Path resolve(String storedName) {
        Path resolved = root.resolve(storedName).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        return resolved;
    }

    private static String ext(String original) {
        int dot = original.lastIndexOf('.');
        return dot >= 0 ? original.substring(dot) : "";
    }

    private static String copyWithMd5(InputStream in, Path target) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        try (InputStream din = new DigestInputStream(in, md)) {
            Files.copy(din, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return HexFormat.of().formatHex(md.digest());
    }

    public record StoredFile(String storedName, String originalName, Path path, long size, String md5) {
    }
}
