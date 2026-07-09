package com.resume.module.resume.service;

import com.resume.config.StorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot);
        }
        String storedName = UUID.randomUUID() + ext;
        Path target = root.resolve(storedName);
        Files.copy(file.getInputStream(), target);
        return new StoredFile(storedName, original, target, file.getSize());
    }

    public Path resolve(String storedName) {
        return root.resolve(storedName).normalize();
    }

    public record StoredFile(String storedName, String originalName, Path path, long size) {
    }
}
