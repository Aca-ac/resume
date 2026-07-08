package com.resume.module.resume.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.BusinessException;
import com.resume.config.StorageProperties;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeDetail;
import com.resume.module.resume.entity.ResumeFile;
import com.resume.module.resume.mapper.ResumeDetailMapper;
import com.resume.module.resume.mapper.ResumeFileMapper;
import com.resume.module.resume.mapper.ResumeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeMapper resumeMapper;
    private final ResumeDetailMapper resumeDetailMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final StorageProperties storageProperties;

    @Value("${app.ai.qwen.api-key:}")
    private String qwenApiKey;

    private static final String QWEN_VL_OCR_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String QWEN_OCR_MODEL = "qwen3.5-ocr";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========== 简历主表 CRUD ==========

    public Resume createResume(Long userId, String title) {
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(title == null || title.isBlank() ? "未命名简历" : title);
        resume.setVersion(1);
        resumeMapper.insert(resume);
        return resume;
    }

    public Resume getResume(Long id, Long userId) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BusinessException(404, "简历不存在");
        }
        return resume;
    }

    public List<Resume> listResumes(Long userId) {
        return resumeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Resume>()
                        .eq(Resume::getUserId, userId)
                        .orderByDesc(Resume::getUpdatedAt));
    }

    public void updateResumeTitle(Long id, Long userId, String title) {
        Resume resume = getResume(id, userId);
        resume.setTitle(title);
        resumeMapper.updateById(resume);
    }

    @Transactional
    public void deleteResume(Long id, Long userId) {
        Resume resume = getResume(id, userId);
        resumeDetailMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResumeDetail>()
                .eq(ResumeDetail::getResumeId, id));
        resumeMapper.deleteById(id);
    }

    // ========== 简历明细分段 CRUD ==========

    public List<ResumeDetail> getDetails(Long resumeId, Long userId) {
        getResume(resumeId, userId);
        return resumeDetailMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResumeDetail>()
                        .eq(ResumeDetail::getResumeId, resumeId)
                        .orderByAsc(ResumeDetail::getSortOrder));
    }

    @Transactional
    public ResumeDetail addDetail(Long resumeId, Long userId, String sectionType, String sectionName, String content, Integer sortOrder) {
        getResume(resumeId, userId);
        ResumeDetail detail = new ResumeDetail();
        detail.setResumeId(resumeId);
        detail.setSectionType(sectionType);
        detail.setSectionName(sectionName);
        detail.setContent(content);
        detail.setSortOrder(sortOrder == null ? 0 : sortOrder);
        resumeDetailMapper.insert(detail);
        return detail;
    }

    @Transactional
    public void updateDetail(Long detailId, Long userId, String content) {
        ResumeDetail detail = resumeDetailMapper.selectById(detailId);
        if (detail == null) {
            throw new BusinessException(404, "分段不存在");
        }
        Resume resume = resumeMapper.selectById(detail.getResumeId());
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限");
        }
        detail.setContent(content);
        resumeDetailMapper.updateById(detail);
    }

    @Transactional
    public void deleteDetail(Long detailId, Long userId) {
        ResumeDetail detail = resumeDetailMapper.selectById(detailId);
        if (detail == null) {
            return;
        }
        Resume resume = resumeMapper.selectById(detail.getResumeId());
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限");
        }
        resumeDetailMapper.deleteById(detailId);
    }

    // ========== 文件上传 ==========

    public ResumeFile uploadFile(Long userId, Long resumeId, MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException(400, "文件名不能为空");
        }

        String ext = getExtension(originalName).toLowerCase();
        if (!List.of("jpg", "jpeg", "png", "pdf", "docx").contains(ext)) {
            throw new BusinessException(400, "不支持的文件类型: " + ext);
        }

        String storageName = UUID.randomUUID().toString() + "." + ext;
        Path uploadPath = Paths.get(storageProperties.getUploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            Path targetPath = uploadPath.resolve(storageName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            ResumeFile resumeFile = new ResumeFile();
            resumeFile.setUserId(userId);
            resumeFile.setResumeId(resumeId);
            resumeFile.setFileType(ext.toUpperCase());
            resumeFile.setFilePath(targetPath.toString());
            resumeFile.setFileSize(file.getSize());
            resumeFile.setOriginalName(originalName);
            resumeFileMapper.insert(resumeFile);
            return resumeFile;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(500, "文件上传失败: " + e.getMessage());
        }
    }

    // ========== OCR识别（JPG图片 -> 通义千问VL-OCR） ==========

    public ResumeFile ocrImage(Long fileId, Long userId) {
        ResumeFile resumeFile = resumeFileMapper.selectById(fileId);
        if (resumeFile == null || !resumeFile.getUserId().equals(userId)) {
            throw new BusinessException(404, "文件不存在");
        }
        if (!List.of("JPG", "JPEG", "PNG").contains(resumeFile.getFileType())) {
            throw new BusinessException(400, "仅支持JPG/PNG图片进行OCR识别");
        }

        if (qwenApiKey == null || qwenApiKey.isBlank()) {
            throw new BusinessException(500, "OCR服务未配置");
        }

        try {
            Path imagePath = Paths.get(resumeFile.getFilePath());
            byte[] imageBytes = Files.readAllBytes(imagePath);
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            Map<String, Object> payload = Map.of(
                "model", QWEN_OCR_MODEL,
                "messages", List.of(
                    Map.of(
                        "role", "user",
                        "content", List.of(
                            Map.of("type", "image_url", "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image)),
                            Map.of("type", "text", "text", "请完整识别图片中的文字内容，保持原有段落结构和格式，不要遗漏任何文字。")
                        )
                    )
                )
            );

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(QWEN_VL_OCR_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + qwenApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("OCR调用失败, status={}, body={}", response.statusCode(), response.body());
                throw new BusinessException(500, "OCR识别服务调用失败");
            }

            JsonNode json = objectMapper.readTree(response.body());
            String ocrText = json.at("/choices/0/message/content").asText();

            if (ocrText == null || ocrText.isBlank()) {
                throw new BusinessException(500, "OCR识别结果为空");
            }

            resumeFile.setOcrText(ocrText);
            resumeFileMapper.updateById(resumeFile);
            return resumeFile;

        } catch (IOException e) {
            log.error("OCR读取图片失败", e);
            throw new BusinessException(500, "OCR识别失败: 图片读取错误");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "OCR识别请求被中断");
        }
    }

    public List<ResumeFile> listFiles(Long userId, Long resumeId) {
        if (resumeId != null) {
            return resumeFileMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResumeFile>()
                            .eq(ResumeFile::getUserId, userId)
                            .eq(ResumeFile::getResumeId, resumeId)
                            .orderByDesc(ResumeFile::getCreatedAt));
        }
        return resumeFileMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResumeFile>()
                        .eq(ResumeFile::getUserId, userId)
                        .orderByDesc(ResumeFile::getCreatedAt));
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf(".");
        return idx == -1 ? "" : filename.substring(idx + 1);
    }
}


