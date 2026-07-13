package com.resume.module.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.common.BusinessException;
import com.resume.config.StorageProperties;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.mapper.ResumeMapper;
import com.resume.module.template.dto.ExportResultVO;
import com.resume.module.template.dto.TemplateExportRequest;
import com.resume.module.template.entity.ExportFile;
import com.resume.module.template.entity.Template;
import com.resume.module.template.mapper.ExportFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportFileService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ExportFileMapper exportFileMapper;
    private final TemplateService templateService;
    private final TemplateRenderService renderService;
    private final TemplateFileStorageService fileStorage;
    private final ResumeMapper resumeMapper;
    private final StorageProperties storageProperties;

    @Transactional
    public ExportResultVO exportWord(Long userId, TemplateExportRequest req) throws IOException {
        Template template = templateService.require(req.getTemplateId());
        Resume resume = requireResume(userId, req.getResumeId());
        Map<String, Object> data = renderService.buildDataMap(userId, req.getResumeId());
        byte[] bytes = renderService.renderDocx(template.getId(), data);
        String fileName = safeName(resume.getTitle()) + "-" + template.getName() + ".docx";
        return persist(userId, resume.getId(), template.getId(), "WORD", bytes, ".docx", fileName);
    }

    @Transactional
    public ExportResultVO exportPdf(Long userId, TemplateExportRequest req) throws IOException {
        Template template = templateService.require(req.getTemplateId());
        Resume resume = requireResume(userId, req.getResumeId());
        Map<String, Object> data = renderService.buildDataMap(userId, req.getResumeId());
        String title = resume.getTitle() == null ? template.getName() : resume.getTitle();
        byte[] bytes = renderService.renderPdf(title, data);
        String fileName = safeName(title) + "-" + template.getName() + ".pdf";
        return persist(userId, resume.getId(), template.getId(), "PDF", bytes, ".pdf", fileName);
    }

    public Resource loadForDownload(Long userId, Long fileId) {
        ExportFile file = exportFileMapper.selectById(fileId);
        if (file == null || !file.getUserId().equals(userId)) {
            throw new BusinessException(404, "文件不存在");
        }
        if (file.getExpireAt() != null && file.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(410, "文件已过期，请重新导出");
        }
        Path path = fileStorage.resolveExportPath(file.getFilePath());
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(404, "文件已丢失");
        }
        return new FileSystemResource(path);
    }

    public ExportFile requireOwned(Long userId, Long fileId) {
        ExportFile file = exportFileMapper.selectById(fileId);
        if (file == null || !file.getUserId().equals(userId)) {
            throw new BusinessException(404, "文件不存在");
        }
        return file;
    }

    @Transactional
    public int cleanExpired() {
        List<ExportFile> expired = exportFileMapper.selectList(
                new LambdaQueryWrapper<ExportFile>().lt(ExportFile::getExpireAt, LocalDateTime.now()));
        int n = 0;
        for (ExportFile f : expired) {
            fileStorage.deleteQuietly(fileStorage.resolveExportPath(f.getFilePath()));
            exportFileMapper.deleteById(f.getId());
            n++;
        }
        if (n > 0) {
            log.info("Cleaned {} expired export files", n);
        }
        return n;
    }

    private ExportResultVO persist(Long userId, Long resumeId, Long templateId,
                                   String fileType, byte[] bytes, String ext, String originalName) throws IOException {
        String relative = fileStorage.saveExportBytes(bytes, ext);
        ExportFile record = new ExportFile();
        record.setUserId(userId);
        record.setResumeId(resumeId);
        record.setTemplateId(templateId);
        record.setFileType(fileType);
        record.setFilePath(relative);
        record.setOriginalName(originalName);
        record.setExpireAt(LocalDateTime.now().plusHours(Math.max(1, storageProperties.getExportTtlHours())));
        exportFileMapper.insert(record);

        ExportResultVO vo = new ExportResultVO();
        vo.setFileId(record.getId());
        vo.setFileType(fileType);
        vo.setOriginalName(originalName);
        vo.setDownloadUrl("/api/v1/files/download/" + record.getId());
        vo.setExpireAt(record.getExpireAt().format(FMT));
        return vo;
    }

    private Resume requireResume(Long userId, Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BusinessException(404, "简历不存在");
        }
        return resume;
    }

    private static String safeName(String name) {
        if (name == null || name.isBlank()) {
            return "resume";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
