package com.resume.module.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.config.StorageProperties;
import com.resume.module.resume.dto.*;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeFile;
import com.resume.module.resume.mapper.ResumeFileMapper;
import com.resume.module.resume.mapper.ResumeMapper;
import com.resume.user_identify.util.JwtTokenUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ResumeService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ResumeMapper resumeMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final FileStorageService fileStorageService;
    private final FileParseService fileParseService;
    private final ResumeExportService exportService;
    private final OcrService ocrService;
    private final StorageProperties storageProperties;

    public ResumeService(ResumeMapper resumeMapper,
                         ResumeFileMapper resumeFileMapper,
                         FileStorageService fileStorageService,
                         FileParseService fileParseService,
                         ResumeExportService exportService,
                         OcrService ocrService,
                         StorageProperties storageProperties) {
        this.resumeMapper = resumeMapper;
        this.resumeFileMapper = resumeFileMapper;
        this.fileStorageService = fileStorageService;
        this.fileParseService = fileParseService;
        this.exportService = exportService;
        this.ocrService = ocrService;
        this.storageProperties = storageProperties;
    }

    public PageResult<ResumeVO> list(Long userId, int page, int size) {
        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resume::getUserId, userId).orderByDesc(Resume::getUpdatedAt);
        Page<Resume> result = resumeMapper.selectPage(new Page<>(page, size), wrapper);
        List<ResumeVO> records = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, result.getTotal(), page, size);
    }

    public ResumeVO get(Long userId, Long id) {
        Resume resume = requireOwned(userId, id);
        return toVO(resume);
    }

    public ResumeVO create(Long userId, ResumeSaveRequest req) {
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(defaultTitle(req.getTitle()));
        resume.setContent(defaultContent(req.getContent()));
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(resume);
        return toVO(resume);
    }

    public ResumeVO update(Long userId, Long id, ResumeSaveRequest req) {
        Resume resume = requireOwned(userId, id);
        if (req.getTitle() != null) {
            resume.setTitle(req.getTitle());
        }
        if (req.getContent() != null) {
            resume.setContent(req.getContent());
        }
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(resume);
        return toVO(resume);
    }

    public void delete(Long userId, Long id) {
        requireOwned(userId, id);
        resumeMapper.deleteById(id);
    }

    public ImportResultVO importFile(Long userId, MultipartFile file) throws IOException {
        validateFile(file);
        String fileType = fileParseService.detectType(file.getOriginalFilename());
        if ("UNKNOWN".equals(fileType)) {
            throw new IllegalArgumentException("仅支持 .doc/.docx/.pdf/.jpg/.jpeg/.png");
        }

        FileStorageService.StoredFile stored = fileStorageService.store(file);
        String text = fileParseService.parse(stored.path(), fileType);
        if (isImage(fileType)) {
            text = "";
        }

        String title = stripExt(stored.originalName());
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(title);
        resume.setContent(text.isBlank() ? "（导入内容为空，图片请执行OCR）" : text);
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(resume);

        ResumeFile record = saveFileRecord(userId, resume.getId(), stored, fileType, text);

        ImportResultVO vo = new ImportResultVO();
        vo.setResumeId(resume.getId());
        vo.setTitle(resume.getTitle());
        vo.setContent(resume.getContent());
        vo.setFileId(record.getId());
        vo.setFileType(fileType);
        return vo;
    }

    public ResumeFile uploadFile(Long userId, Long resumeId, MultipartFile file) throws IOException {
        requireOwned(userId, resumeId);
        validateFile(file);
        String fileType = fileParseService.detectType(file.getOriginalFilename());
        FileStorageService.StoredFile stored = fileStorageService.store(file);
        return saveFileRecord(userId, resumeId, stored, fileType, null);
    }

    public OcrResultVO runOcr(Long userId, Long fileId) {
        ResumeFile file = requireOwnedFile(userId, fileId);
        String type = file.getFileType();
        if (!isImage(type)) {
            throw new IllegalArgumentException("仅支持图片OCR");
        }
        String text = ocrService.recognize(fileStorageService.resolve(file.getFilePath()), type);
        file.setOcrText(text);
        resumeFileMapper.updateById(file);

        Resume resume = resumeMapper.selectById(file.getResumeId());
        if (resume != null && userId.equals(resume.getUserId())) {
            resume.setContent(text);
            resume.setUpdatedAt(LocalDateTime.now());
            resumeMapper.updateById(resume);
        }

        OcrResultVO vo = new OcrResultVO();
        vo.setFileId(fileId);
        vo.setOcrText(text);
        return vo;
    }

    public byte[] exportPdf(Long userId, Long id) throws IOException {
        Resume resume = requireOwned(userId, id);
        return exportService.exportPdf(resume.getTitle(), resume.getContent());
    }

    public byte[] exportDocx(Long userId, Long id) throws IOException {
        Resume resume = requireOwned(userId, id);
        return exportService.exportDocx(resume.getTitle(), resume.getContent());
    }

    public byte[] exportText(Long userId, Long id) {
        Resume resume = requireOwned(userId, id);
        return exportService.exportText(resume.getTitle(), resume.getContent());
    }

    public Long resolveUserId(String authorization) {
        Long userId = JwtTokenUtil.getUserIdFromAuthorization(authorization);
        return userId == null ? 1L : userId;
    }

    private Resume requireOwned(Long userId, Long id) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null || !userId.equals(resume.getUserId())) {
            throw new IllegalArgumentException("简历不存在");
        }
        return resume;
    }

    private ResumeFile requireOwnedFile(Long userId, Long fileId) {
        ResumeFile file = resumeFileMapper.selectById(fileId);
        if (file == null || !userId.equals(file.getUserId())) {
            throw new IllegalArgumentException("文件不存在");
        }
        return file;
    }

    private ResumeFile saveFileRecord(Long userId, Long resumeId, FileStorageService.StoredFile stored,
                                      String fileType, String ocrText) {
        ResumeFile record = new ResumeFile();
        record.setUserId(userId);
        record.setResumeId(resumeId);
        record.setFileType(fileType);
        record.setFilePath(stored.storedName());
        record.setFileSize(stored.size());
        record.setOriginalName(stored.originalName());
        record.setOcrText(ocrText);
        record.setCreatedAt(LocalDateTime.now());
        resumeFileMapper.insert(record);
        return record;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (file.getSize() > storageProperties.getMaxFileSize()) {
            throw new IllegalArgumentException("文件大小超过限制");
        }
    }

    private ResumeVO toVO(Resume resume) {
        ResumeVO vo = new ResumeVO();
        vo.setId(resume.getId());
        vo.setTitle(resume.getTitle());
        vo.setContent(resume.getContent());
        vo.setUpdatedAt(resume.getUpdatedAt() == null ? null : resume.getUpdatedAt().format(FMT));
        return vo;
    }

    private String defaultTitle(String title) {
        return title == null || title.isBlank() ? "未命名简历" : title;
    }

    private String defaultContent(String content) {
        return content == null ? "" : content;
    }

    private String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private boolean isImage(String type) {
        return "JPG".equalsIgnoreCase(type) || "JPEG".equalsIgnoreCase(type) || "PNG".equalsIgnoreCase(type);
    }
}
