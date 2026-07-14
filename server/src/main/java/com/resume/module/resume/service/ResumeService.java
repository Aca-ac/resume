package com.resume.module.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.common.BusinessException;
import com.resume.config.StorageProperties;
import com.resume.module.resume.dto.ChunkUploadVO;
import com.resume.module.resume.dto.ImportResultVO;
import com.resume.module.resume.dto.ResumeSaveRequest;
import com.resume.module.resume.dto.ResumeVO;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.entity.ResumeDetail;
import com.resume.module.resume.entity.ResumeFile;
import com.resume.module.resume.mapper.ResumeDetailMapper;
import com.resume.module.resume.mapper.ResumeFileMapper;
import com.resume.module.resume.mapper.ResumeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_IMPORT = "IMPORT";
    private static final String SECTION_SUMMARY = "SUMMARY";

    private final ResumeMapper resumeMapper;
    private final ResumeDetailMapper resumeDetailMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final FileStorageService fileStorageService;
    private final FileParseService fileParseService;
    private final ResumeExportService exportService;
    private final OcrService ocrService;
    private final ChunkUploadService chunkUploadService;
    private final StorageProperties storageProperties;
    private final ResumeOptimizeService optimizeService;
    private final ResumePhotoService resumePhotoService;

    // ========== 简历 CRUD（正文存 resume_details.SUMMARY）==========

    public ResumeVO createResume(Long userId, String title) {
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(defaultTitle(title));
        resume.setSourceType(SOURCE_MANUAL);
        resume.setVersion(1);
        resumeMapper.insert(resume);
        saveSummaryContent(resume.getId(), "");
        return toVO(resume, "");
    }

    public ResumeVO createResume(Long userId, ResumeSaveRequest req) {
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(defaultTitle(req.getTitle()));
        resume.setSourceType(SOURCE_MANUAL);
        resume.setVersion(1);
        resumeMapper.insert(resume);
        String content = defaultContent(req.getContent());
        saveSummaryContent(resume.getId(), content);
        return toVO(resume, content);
    }

    public ResumeVO getResumeVo(Long id, Long userId) {
        Resume resume = getResume(id, userId);
        return toVO(resume, loadSummaryContent(id));
    }

    public Resume getResume(Long id, Long userId) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BusinessException(404, "简历不存在");
        }
        return resume;
    }

    public List<ResumeVO> listResumes(Long userId) {
        List<Resume> rows = resumeMapper.selectList(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .orderByDesc(Resume::getUpdatedAt));
        return rows.stream()
                .map(r -> toVO(r, previewSummary(r.getId())))
                .toList();
    }

    public void updateResumeTitle(Long id, Long userId, String title) {
        Resume resume = getResume(id, userId);
        resume.setTitle(title);
        resumeMapper.updateById(resume);
    }

    public ResumeVO updateResume(Long id, Long userId, ResumeSaveRequest req) {
        Resume resume = getResume(id, userId);
        if (req.getTitle() != null) {
            resume.setTitle(req.getTitle());
        }
        if (req.getContent() != null) {
            saveSummaryContent(id, req.getContent());
        }
        resume.setVersion(resume.getVersion() == null ? 1 : resume.getVersion() + 1);
        resumeMapper.updateById(resume);
        return toVO(resume, loadSummaryContent(id));
    }

    @Transactional
    public void deleteResume(Long id, Long userId) {
        Resume resume = getResume(id, userId);
        List<ResumeFile> files = resumeFileMapper.selectList(
                new LambdaQueryWrapper<ResumeFile>().eq(ResumeFile::getResumeId, id));
        for (ResumeFile file : files) {
            deletePhysicalFile(file.getFilePath());
        }
        resumePhotoService.deletePhotoFile(resume.getPhotoPath());
        resumeDetailMapper.delete(new LambdaQueryWrapper<ResumeDetail>().eq(ResumeDetail::getResumeId, id));
        resumeFileMapper.delete(new LambdaQueryWrapper<ResumeFile>().eq(ResumeFile::getResumeId, id));
        resumeMapper.deleteById(id);
    }

    // ========== 明细分段 CRUD ==========

    public List<ResumeDetail> getDetails(Long resumeId, Long userId) {
        getResume(resumeId, userId);
        return resumeDetailMapper.selectList(new LambdaQueryWrapper<ResumeDetail>()
                .eq(ResumeDetail::getResumeId, resumeId)
                .orderByAsc(ResumeDetail::getSortOrder));
    }

    @Transactional
    public ResumeDetail addDetail(Long resumeId, Long userId, String sectionType, String sectionName,
                                  String content, Integer sortOrder) {
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
            throw new BusinessException(403, "无权访问");
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
            throw new BusinessException(403, "无权访问");
        }
        resumeDetailMapper.deleteById(detailId);
    }

    // ========== 导入 / 分片 / 导出 ==========

    public ImportResultVO importFile(Long userId, MultipartFile file) throws IOException {
        validateFile(file);
        String fileType = fileParseService.detectType(file.getOriginalFilename());
        if ("UNKNOWN".equals(fileType)) {
            throw new BusinessException(400, "仅支持 .doc/.docx/.pdf/.jpg/.jpeg/.png");
        }
        String md5 = ChunkUploadService.md5(file);
        FileStorageService.StoredFile stored = findQuickUpload(userId, md5)
                .map(f -> new FileStorageService.StoredFile(f.getFilePath(), file.getOriginalFilename(),
                        resolveFilePath(f.getFilePath()), f.getFileSize(), md5))
                .orElseGet(() -> {
                    try {
                        return fileStorageService.store(file);
                    } catch (IOException e) {
                        throw new BusinessException(500, "文件保存失败");
                    }
                });
        return buildImportResult(userId, stored, fileType);
    }

    public ImportResultVO mergeChunks(Long userId, String uploadId, String filename, int totalChunks) throws IOException {
        FileStorageService.StoredFile stored = chunkUploadService.merge(uploadId, filename, totalChunks);
        String fileType = fileParseService.detectType(filename);
        if ("UNKNOWN".equals(fileType)) {
            throw new BusinessException(400, "不支持的文件类型");
        }
        validateSize(stored.size());
        return buildImportResult(userId, stored, fileType);
    }

    public ChunkUploadVO uploadChunk(String uploadId, int chunkIndex, MultipartFile chunk) throws IOException {
        validateSize(chunk.getSize());
        chunkUploadService.saveChunk(uploadId, chunkIndex, chunk);
        ChunkUploadVO vo = new ChunkUploadVO();
        vo.setUploadId(uploadId);
        vo.setChunkIndex(chunkIndex);
        vo.setReceived(true);
        return vo;
    }

    public byte[] exportPdf(Long userId, Long id) throws IOException {
        Resume resume = getResume(id, userId);
        return exportService.exportPdf(resume.getTitle(), loadSummaryContent(id));
    }

    public byte[] exportDocx(Long userId, Long id) throws IOException {
        Resume resume = getResume(id, userId);
        return exportService.exportDocx(resume.getTitle(), loadSummaryContent(id));
    }

    public byte[] exportText(Long userId, Long id) {
        Resume resume = getResume(id, userId);
        return exportService.exportText(resume.getTitle(), loadSummaryContent(id));
    }

    /**
     * AI 按目标职位优化简历正文；不覆盖原简历，仅返回优化结果供前端预览。
     */
    public ResumeVO optimizeResume(Long userId, Long id, String targetRole) {
        Resume resume = getResume(id, userId);
        String original = loadSummaryContent(id);
        String optimized = optimizeService.optimize(original, targetRole);
        return toVO(resume, optimized);
    }

    // ========== 文件上传与 OCR ==========

    public ResumeFile uploadFile(Long userId, Long resumeId, MultipartFile file) {
        getResume(resumeId, userId);
        validateFile(file);
        String fileType = fileParseService.detectType(file.getOriginalFilename());
        if ("UNKNOWN".equals(fileType)) {
            throw new BusinessException(400, "不支持的文件类型");
        }
        try {
            FileStorageService.StoredFile stored = fileStorageService.store(file);
            return saveFileRecord(userId, resumeId, stored, fileType, null, "PENDING");
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(500, "文件上传失败: " + e.getMessage());
        }
    }

    public ResumeFile extractFileText(Long fileId, Long userId) {
        ResumeFile resumeFile = requireOwnedFile(userId, fileId);
        try {
            String text = fileParseService.parse(resolveFilePath(resumeFile.getFilePath()), resumeFile.getFileType());
            resumeFile.setOcrText(text);
            resumeFile.setParseStatus(text.isBlank() ? "FAILED" : "DONE");
            resumeFileMapper.updateById(resumeFile);
            syncSummaryFromFile(resumeFile, text);
            return resumeFile;
        } catch (IOException e) {
            log.error("File text extraction failed, fileId={}", fileId, e);
            throw new BusinessException(500, "文件内容提取失败: " + e.getMessage());
        }
    }

    public ResumeFile ocrImage(Long fileId, Long userId) {
        ResumeFile resumeFile = requireOwnedFile(userId, fileId);
        if (!isImage(resumeFile.getFileType())) {
            throw new BusinessException(400, "仅支持 JPG/PNG 图片进行 OCR 识别");
        }
        String text = ocrService.recognize(resolveFilePath(resumeFile.getFilePath()), resumeFile.getFileType());
        resumeFile.setOcrText(text);
        resumeFile.setParseStatus(text.startsWith("【") ? "OCR_FALLBACK" : "DONE");
        resumeFileMapper.updateById(resumeFile);
        syncSummaryFromFile(resumeFile, text);
        return resumeFile;
    }

    public List<ResumeFile> listFiles(Long userId, Long resumeId) {
        LambdaQueryWrapper<ResumeFile> wrapper = new LambdaQueryWrapper<ResumeFile>()
                .eq(ResumeFile::getUserId, userId)
                .orderByDesc(ResumeFile::getCreatedAt);
        if (resumeId != null) {
            wrapper.eq(ResumeFile::getResumeId, resumeId);
        }
        return resumeFileMapper.selectList(wrapper);
    }

    // ========== private helpers ==========

    private ImportResultVO buildImportResult(Long userId, FileStorageService.StoredFile stored,
                                             String fileType) throws IOException {
        String text;
        String parseStatus;

        if (isImage(fileType)) {
            text = ocrService.recognize(stored.path(), fileType);
            parseStatus = text.startsWith("【") ? "OCR_FALLBACK" : "DONE";
        } else {
            text = parseText(stored, fileType);
            parseStatus = text.isBlank() ? "FAILED" : "DONE";
        }

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(stripExt(stored.originalName()));
        resume.setSourceType(SOURCE_IMPORT);
        resume.setVersion(1);
        resumeMapper.insert(resume);

        String content = text.isBlank() ? "（导入内容为空，请手动编辑或重新 OCR）" : text;
        saveSummaryContent(resume.getId(), content);
        ResumeFile record = saveFileRecord(userId, resume.getId(), stored, fileType, text, parseStatus);

        ImportResultVO vo = new ImportResultVO();
        vo.setResumeId(resume.getId());
        vo.setTitle(resume.getTitle());
        vo.setContent(content);
        vo.setFileId(record.getId());
        vo.setFileType(fileType);
        vo.setParseStatus(parseStatus);
        return vo;
    }

    private String parseText(FileStorageService.StoredFile stored, String fileType) throws IOException {
        try {
            return fileParseService.parse(stored.path(), fileType);
        } catch (Exception e) {
            log.warn("Parse failed for {}: {}", stored.originalName(), e.getMessage());
            return "";
        }
    }

    private Optional<ResumeFile> findQuickUpload(Long userId, String md5) {
        if (md5 == null || md5.isBlank()) {
            return Optional.empty();
        }
        ResumeFile hit = resumeFileMapper.selectOne(new LambdaQueryWrapper<ResumeFile>()
                .eq(ResumeFile::getUserId, userId)
                .eq(ResumeFile::getFileMd5, md5)
                .last("LIMIT 1"));
        return Optional.ofNullable(hit);
    }

    private String loadSummaryContent(Long resumeId) {
        ResumeDetail detail = resumeDetailMapper.selectOne(new LambdaQueryWrapper<ResumeDetail>()
                .eq(ResumeDetail::getResumeId, resumeId)
                .eq(ResumeDetail::getSectionType, SECTION_SUMMARY)
                .last("LIMIT 1"));
        return detail == null || detail.getContent() == null ? "" : detail.getContent();
    }

    private String previewSummary(Long resumeId) {
        String full = loadSummaryContent(resumeId);
        if (full.length() <= 200) {
            return full;
        }
        return full.substring(0, 200) + "...";
    }

    private void saveSummaryContent(Long resumeId, String content) {
        ResumeDetail existing = resumeDetailMapper.selectOne(new LambdaQueryWrapper<ResumeDetail>()
                .eq(ResumeDetail::getResumeId, resumeId)
                .eq(ResumeDetail::getSectionType, SECTION_SUMMARY)
                .last("LIMIT 1"));
        if (existing == null) {
            ResumeDetail detail = new ResumeDetail();
            detail.setResumeId(resumeId);
            detail.setSectionType(SECTION_SUMMARY);
            detail.setSectionName("正文");
            detail.setContent(content == null ? "" : content);
            detail.setSortOrder(0);
            resumeDetailMapper.insert(detail);
        } else {
            existing.setContent(content == null ? "" : content);
            resumeDetailMapper.updateById(existing);
        }
    }

    private void syncSummaryFromFile(ResumeFile file, String text) {
        if (file.getResumeId() == null || text == null || text.isBlank()) {
            return;
        }
        saveSummaryContent(file.getResumeId(), text);
        Resume resume = resumeMapper.selectById(file.getResumeId());
        if (resume != null) {
            resume.setVersion(resume.getVersion() == null ? 1 : resume.getVersion() + 1);
            resumeMapper.updateById(resume);
        }
    }

    private ResumeFile requireOwnedFile(Long userId, Long fileId) {
        ResumeFile file = resumeFileMapper.selectById(fileId);
        if (file == null || !file.getUserId().equals(userId)) {
            throw new BusinessException(404, "文件不存在");
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
        resumeFileMapper.insert(record);
        return record;
    }

    private void deletePhysicalFile(String filePath) {
        try {
            Files.deleteIfExists(resolveFilePath(filePath));
        } catch (Exception e) {
            log.warn("删除文件失败: {}", filePath);
        }
    }

    private Path resolveFilePath(String filePath) {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            return path;
        }
        return fileStorageService.resolve(filePath);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        validateSize(file.getSize());
    }

    private void validateSize(long size) {
        if (size > storageProperties.getMaxFileSize()) {
            throw new BusinessException(400, "文件大小超过限制（最大 100MB）");
        }
    }

    private ResumeVO toVO(Resume resume, String content) {
        ResumeVO vo = new ResumeVO();
        vo.setId(resume.getId());
        vo.setTitle(resume.getTitle());
        vo.setSourceType(resume.getSourceType() == null ? SOURCE_MANUAL : resume.getSourceType());
        vo.setContent(content);
        vo.setPhotoUrl(ResumePhotoService.photoUrl(resume.getId(), resume.getPhotoPath()));
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
