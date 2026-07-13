package com.resume.module.resume.service;

import com.resume.common.BusinessException;
import com.resume.config.ExportProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LibreOfficePdfConverter {

    private final ExportProperties exportProperties;

    public byte[] convertDocxToPdf(byte[] docxBytes, String baseName) throws IOException {
        if (docxBytes == null || docxBytes.length == 0) {
            throw new BusinessException(400, "Word 文件内容为空");
        }
        Path tempDir = Files.createTempDirectory("resume-export-");
        String safeBase = baseName == null || baseName.isBlank() ? "resume" : sanitizeFileName(baseName);
        Path docxPath = tempDir.resolve(safeBase + ".docx");
        Path pdfPath = tempDir.resolve(safeBase + ".pdf");
        try {
            Files.write(docxPath, docxBytes);
            runLibreOffice(tempDir, docxPath);
            if (!Files.exists(pdfPath)) {
                throw new BusinessException(500, "PDF 转换失败，未生成输出文件。请确认已安装 LibreOffice 并加入 PATH");
            }
            return Files.readAllBytes(pdfPath);
        } finally {
            deleteQuietly(docxPath);
            deleteQuietly(pdfPath);
            deleteQuietly(tempDir);
        }
    }

    private void runLibreOffice(Path outDir, Path docxPath) throws IOException {
        String command = resolveLibreOfficeCommand();
        ProcessBuilder builder = new ProcessBuilder(
                command,
                "--headless",
                "--norestore",
                "--convert-to", "pdf",
                "--outdir", outDir.toAbsolutePath().toString(),
                docxPath.toAbsolutePath().toString()
        );
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes());
        boolean finished;
        try {
            finished = process.waitFor(exportProperties.getConvertTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new BusinessException(500, "PDF 转换被中断");
        }
        if (!finished) {
            process.destroyForcibly();
            throw new BusinessException(500, "PDF 转换超时，请检查 LibreOffice 是否可用");
        }
        if (process.exitValue() != 0) {
            log.warn("LibreOffice convert failed, exit={}, output={}", process.exitValue(), output);
            throw new BusinessException(500, "PDF 转换失败，请确认 LibreOffice 已安装（命令: " + command + "）");
        }
    }

    private String resolveLibreOfficeCommand() {
        String configured = exportProperties.getLibreofficeCommand();
        if (configured != null && !configured.isBlank() && !"soffice".equals(configured.trim())) {
            return configured.trim();
        }
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            Path windowsCom = Path.of("C:/Program Files/LibreOffice/program/soffice.com");
            if (Files.isRegularFile(windowsCom)) {
                return windowsCom.toString();
            }
        }
        return configured == null || configured.isBlank() ? "soffice" : configured.trim();
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void deleteQuietly(Path path) {
        try {
            if (path != null) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            log.debug("Failed to delete temp file {}: {}", path, e.getMessage());
        }
    }
}
