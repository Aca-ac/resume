package com.resume.module.template.service;

import com.resume.common.BusinessException;
import com.resume.config.StorageProperties;
import com.resume.module.template.dto.TemplateUploadVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TemplateFileStorageService {

    private final Path templateRoot;
    private final Path exportRoot;
    private final long maxFileSize;

    public TemplateFileStorageService(StorageProperties properties) throws IOException {
        this.templateRoot = Paths.get(properties.getTemplateDir()).toAbsolutePath().normalize();
        this.exportRoot = Paths.get(properties.getExportDir()).toAbsolutePath().normalize();
        this.maxFileSize = properties.getMaxFileSize();
        Files.createDirectories(templateRoot.resolve("docx"));
        Files.createDirectories(templateRoot.resolve("preview"));
        Files.createDirectories(exportRoot);
    }

    public TemplateUploadVO storeTemplateDocx(MultipartFile file) throws IOException {
        validate(file, List.of("docx"), "仅支持 .docx 模板");
        return storeUnder(file, "docx", "DOCX");
    }

    public TemplateUploadVO storePreview(MultipartFile file) throws IOException {
        validate(file, List.of("jpg", "jpeg", "png"), "预览图仅支持 jpg/png");
        return storeUnder(file, "preview", "PREVIEW");
    }

    public Path resolveTemplatePath(String relativePath) {
        Path path = templateRoot.resolve(relativePath).normalize();
        if (!path.startsWith(templateRoot)) {
            throw new BusinessException(400, "非法模板路径");
        }
        return path;
    }

    public Path resolveExportPath(String relativePath) {
        Path path = exportRoot.resolve(relativePath).normalize();
        if (!path.startsWith(exportRoot)) {
            throw new BusinessException(400, "非法导出路径");
        }
        return path;
    }

    public String saveExportBytes(byte[] data, String ext) throws IOException {
        String name = UUID.randomUUID() + (ext.startsWith(".") ? ext : "." + ext);
        Path target = exportRoot.resolve(name);
        Files.write(target, data);
        return name;
    }

    public void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除文件失败: {}", path);
        }
    }

    public void deleteTemplateRelative(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        deleteQuietly(resolveTemplatePath(relativePath));
    }

    private TemplateUploadVO storeUnder(MultipartFile file, String subDir, String type) throws IOException {
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = extension(original);
        String stored = UUID.randomUUID() + ext;
        Path target = templateRoot.resolve(subDir).resolve(stored);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        String relative = subDir + "/" + stored;
        TemplateUploadVO vo = new TemplateUploadVO();
        vo.setType(type);
        vo.setPath(relative);
        vo.setUrl("/uploads/templates/" + relative.replace('\\', '/'));
        vo.setOriginalName(original);
        vo.setSize(file.getSize());
        return vo;
    }

    private void validate(MultipartFile file, List<String> allowed, String message) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        if (file.getSize() > maxFileSize) {
            throw new BusinessException(400, "文件过大");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        boolean ok = allowed.stream().anyMatch(a -> name.endsWith("." + a));
        if (!ok) {
            throw new BusinessException(400, message);
        }
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }
}
