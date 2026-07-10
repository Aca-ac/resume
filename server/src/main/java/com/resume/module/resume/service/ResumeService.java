package com.resume.module.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.config.StorageProperties;
import com.resume.module.resume.dto.*;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeDetail;
import com.resume.module.resume.entity.ResumeFile;
import com.resume.module.resume.mapper.ResumeDetailMapper;
import com.resume.module.resume.mapper.ResumeFileMapper;
import com.resume.module.resume.mapper.ResumeMapper;
import com.resume.user_identify.util.JwtTokenUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ResumeService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_IMPORT = "IMPORT";
    private static final int DETAIL_MAX = 16_000;

    private final ResumeMapper resumeMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final ResumeDetailMapper resumeDetailMapper;
    private final FileStorageService fileStorageService;
    private final FileParseService fileParseService;
    private final ResumeExportService exportService;
    private final OcrService ocrService;
    private final ChunkUploadService chunkUploadService;
    private final StorageProperties storageProperties;

    public ResumeService(ResumeMapper resumeMapper,
                         ResumeFileMapper resumeFileMapper,
                         ResumeDetailMapper resumeDetailMapper,
                         FileStorageService fileStorageService,
                         FileParseService fileParseService,
                         ResumeExportService exportService,
                         OcrService ocrService,
                         ChunkUploadService chunkUploadService,
                         StorageProperties storageProperties) {
        this.resumeMapper = resumeMapper;
        this.resumeFileMapper = resumeFileMapper;
        this.resumeDetailMapper = resumeDetailMapper;
        this.fileStorageService = fileStorageService;
        this.fileParseService = fileParseService;
        this.exportService = exportService;
        this.ocrService = ocrService;
        this.chunkUploadService = chunkUploadService;
        this.storageProperties = storageProperties;
    }

    /** 新建 + 导入统一列表：只查 resumes 主表 */
    public PageResult<ResumeVO> list(Long userId, int page, int size) {
        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resume::getUserId, userId).orderByDesc(Resume::getUpdatedAt);
        Page<Resume> result = resumeMapper.selectPage(new Page<>(page, size), wrapper);
        List<ResumeVO> records = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, result.getTotal(), page, size);
    }

    public ResumeVO get(Long userId, Long id) {
        return toVO(requireOwned(userId, id));
    }

    public ResumeVO create(Long userId, ResumeSaveRequest req) {
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(defaultTitle(req.getTitle()));
        resume.setSourceType(SOURCE_MANUAL);
        resume.setContent(defaultContent(req.getContent()));
        resume.setVersion(1);
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(resume);
        upsertSummaryDetail(resume.getId(), resume.getContent());
        return toVO(resume);
    }

    public ResumeVO update(Long userId, Long id, ResumeSaveRequest req) {
        Resume resume = requireOwned(userId, id);
        if (req.getTitle() != null) {
            resume.setTitle(req.getTitle());
        }
        if (req.getContent() != null) {
            resume.setContent(req.getContent());
            upsertSummaryDetail(id, req.getContent());
        }
        resume.setVersion(resume.getVersion() == null ? 1 : resume.getVersion() + 1);
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(resume);
        return toVO(resume);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        requireOwned(userId, id);
        resumeDetailMapper.delete(new LambdaQueryWrapper<ResumeDetail>().eq(ResumeDetail::getResumeId, id));
        resumeFileMapper.delete(new LambdaQueryWrapper<ResumeFile>().eq(ResumeFile::getResumeId, id));
        resumeMapper.deleteById(id);
    }

    public ImportResultVO importFile(Long userId, MultipartFile file) throws IOException {
        validateFile(file);
        String fileType = fileParseService.detectType(file.getOriginalFilename());
        if ("UNKNOWN".equals(fileType)) {
            throw new IllegalArgumentException("仅支持 .doc/.docx/.pdf/.jpg/.jpeg/.png");
        }
        String md5 = ChunkUploadService.md5(file);
        FileStorageService.StoredFile stored = findQuickUpload(userId, md5)
                .map(f -> new FileStorageService.StoredFile(f.getFilePath(), file.getOriginalFilename(),
                        fileStorageService.resolve(f.getFilePath()), f.getFileSize(), md5))
                .orElseGet(() -> {
                    try {
                        return fileStorageService.store(file);
                    } catch (IOException e) {
                        throw new IllegalStateException("文件保存失败", e);
                    }
                });
        return buildImportResult(userId, stored, fileType, md5);
    }

    public ImportResultVO mergeChunks(Long userId, String uploadId, String filename, int totalChunks) throws IOException {
        FileStorageService.StoredFile stored = chunkUploadService.merge(uploadId, filename, totalChunks);
        String fileType = fileParseService.detectType(filename);
        if ("UNKNOWN".equals(fileType)) {
            throw new IllegalArgumentException("不支持的文件类型");
        }
        validateSize(stored.size());
        return buildImportResult(userId, stored, fileType, stored.md5());
    }

    public ChunkUploadVO uploadChunk(String uploadId, int chunkIndex, MultipartFile chunk) throws IOException {
        chunkUploadService.saveChunk(uploadId, chunkIndex, chunk);
        ChunkUploadVO vo = new ChunkUploadVO();
        vo.setUploadId(uploadId);
        vo.setChunkIndex(chunkIndex);
        vo.setReceived(true);
        return vo;
    }

    public ResumeFile uploadFile(Long userId, Long resumeId, MultipartFile file) throws IOException {
        requireOwned(userId, resumeId);
        validateFile(file);
        String fileType = fileParseService.detectType(file.getOriginalFilename());
        FileStorageService.StoredFile stored = fileStorageService.store(file);
        return saveFileRecord(userId, resumeId, stored, fileType, null, "PENDING");
    }

    public OcrResultVO runOcr(Long userId, Long fileId) {
        ResumeFile file = requireOwnedFile(userId, fileId);
        String text = ocrService.recognize(fileStorageService.resolve(file.getFilePath()), file.getFileType());
        file.setOcrText(text);
        file.setParseStatus(text.startsWith("【") ? "OCR_FALLBACK" : "DONE");
        resumeFileMapper.updateById(file);

        if (file.getResumeId() != null) {
            Resume resume = resumeMapper.selectById(file.getResumeId());
            if (resume != null && userId.equals(resume.getUserId())) {
                resume.setContent(text);
                resume.setVersion(resume.getVersion() == null ? 1 : resume.getVersion() + 1);
                resume.setUpdatedAt(LocalDateTime.now());
                resumeMapper.updateById(resume);
                upsertSummaryDetail(resume.getId(), text);
            }
        }

        OcrResultVO vo = new OcrResultVO();
        vo.setFileId(fileId);
        vo.setOcrText(text);
        return vo;
    }

    public byte[] exportPdf(Long userId, Long id) throws IOException {
        Resume resume = requireOwned(userId, id);
        return exportService.exportPdf(resume.getTitle(), safeContent(resume));
    }

    public byte[] exportDocx(Long userId, Long id) throws IOException {
        Resume resume = requireOwned(userId, id);
        return exportService.exportDocx(resume.getTitle(), safeContent(resume));
    }

    public byte[] exportText(Long userId, Long id) {
        Resume resume = requireOwned(userId, id);
        return exportService.exportText(resume.getTitle(), safeContent(resume));
    }

    public Long resolveUserId(String authorization) {
        Long userId = JwtTokenUtil.getUserIdFromAuthorization(authorization);
        return userId == null ? 1L : userId;
    }

    private ImportResultVO buildImportResult(Long userId, FileStorageService.StoredFile stored,
                                             String fileType, String md5) throws IOException {
        String text = parseText(stored, fileType);

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(stripExt(stored.originalName()));
        resume.setSourceType(SOURCE_IMPORT);
        resume.setContent(text.isBlank() ? "（导入内容为空，图片请执行 OCR）" : text);
        resume.setVersion(1);
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(resume);

        String status = isImage(fileType) ? "PENDING" : (text.isBlank() ? "FAILED" : "DONE");
        ResumeFile record = saveFileRecord(userId, resume.getId(), stored, fileType, text, status);
        upsertSummaryDetail(resume.getId(), resume.getContent());

        ImportResultVO vo = new ImportResultVO();
        vo.setResumeId(resume.getId());
        vo.setTitle(resume.getTitle());
        vo.setContent(resume.getContent());
        vo.setFileId(record.getId());
        vo.setFileType(fileType);
        return vo;
    }

    private String parseText(FileStorageService.StoredFile stored, String fileType) throws IOException {
        if (isImage(fileType)) {
            return "";
        }
        try {
            return fileParseService.parse(stored.path(), fileType);
        } catch (Exception e) {
            return "";
        }
    }

    private java.util.Optional<ResumeFile> findQuickUpload(Long userId, String md5) {
        if (md5 == null || md5.isBlank()) {
            return java.util.Optional.empty();
        }
        ResumeFile hit = resumeFileMapper.selectOne(new LambdaQueryWrapper<ResumeFile>()
                .eq(ResumeFile::getUserId, userId)
                .eq(ResumeFile::getFileMd5, md5)
                .last("LIMIT 1"));
        return java.util.Optional.ofNullable(hit);
    }

    private void upsertSummaryDetail(Long resumeId, String content) {
        if (content == null) {
            return;
        }
        String safe = content.length() > DETAIL_MAX ? content.substring(0, DETAIL_MAX) : content;
        ResumeDetail existing = resumeDetailMapper.selectOne(new LambdaQueryWrapper<ResumeDetail>()
                .eq(ResumeDetail::getResumeId, resumeId)
                .eq(ResumeDetail::getSectionType, "SUMMARY")
                .last("LIMIT 1"));
        if (existing == null) {
            ResumeDetail detail = new ResumeDetail();
            detail.setResumeId(resumeId);
            detail.setSectionType("SUMMARY");
            detail.setSectionName("正文");
            detail.setContent(safe);
            detail.setSortOrder(0);
            detail.setCreatedAt(LocalDateTime.now());
            detail.setUpdatedAt(LocalDateTime.now());
            resumeDetailMapper.insert(detail);
        } else {
            existing.setContent(safe);
            existing.setUpdatedAt(LocalDateTime.now());
            resumeDetailMapper.updateById(existing);
        }
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
                                      String fileType, String ocrText, String parseStatus) {
        ResumeFile record = new ResumeFile();
        record.setUserId(userId);
        record.setResumeId(resumeId);
        record.setFileType(fileType);
        record.setFilePath(stored.storedName());
        record.setFileSize(stored.size());
        record.setFileMd5(stored.md5());
        record.setOriginalName(stored.originalName());
        record.setOcrText(ocrText);
        record.setParseStatus(parseStatus);
        record.setCreatedAt(LocalDateTime.now());
        resumeFileMapper.insert(record);
        return record;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        validateSize(file.getSize());
    }

    private void validateSize(long size) {
        if (size > storageProperties.getMaxFileSize()) {
            throw new IllegalArgumentException("文件大小超过限制（最大 100MB）");
        }
    }

    private ResumeVO toVO(Resume resume) {
        ResumeVO vo = new ResumeVO();
        vo.setId(resume.getId());
        vo.setTitle(resume.getTitle());
        vo.setSourceType(resume.getSourceType() == null ? SOURCE_MANUAL : resume.getSourceType());
        vo.setContent(resume.getContent());
        vo.setUpdatedAt(resume.getUpdatedAt() == null ? null : resume.getUpdatedAt().format(FMT));
        return vo;
    }

    private String safeContent(Resume resume) {
        return resume.getContent() == null ? "" : resume.getContent();
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
