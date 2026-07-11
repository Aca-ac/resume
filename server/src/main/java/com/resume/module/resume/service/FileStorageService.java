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
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = ext(original);
        String storedName = UUID.randomUUID() + ext;
        Path target = root.resolve(storedName);
        String md5 = copyWithMd5(file.getInputStream(), target);
        return new StoredFile(storedName, original, target, file.getSize(), md5);
    }

    public StoredFile store(StoredFile existing) {
        return existing;
    }

    public Path resolve(String storedName) {
        return root.resolve(storedName).normalize();
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
