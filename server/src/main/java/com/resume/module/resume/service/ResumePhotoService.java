package com.resume.module.resume.service;

import com.resume.common.BusinessException;
import com.resume.module.resume.dto.ResumeVO;
import com.resume.module.resume.entity.Resume;
import com.resume.module.resume.mapper.ResumeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
public class ResumePhotoService {

    private static final String PHOTO_SUBDIR = "photos";
    private static final long MAX_PHOTO_BYTES = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/jpg"
    );

    private final ResumeMapper resumeMapper;
    private final FileStorageService fileStorageService;
    private final ResumeService resumeService;

    public ResumePhotoService(ResumeMapper resumeMapper,
                              FileStorageService fileStorageService,
                              @Lazy ResumeService resumeService) {
        this.resumeMapper = resumeMapper;
        this.fileStorageService = fileStorageService;
        this.resumeService = resumeService;
    }

    @Transactional
    public ResumeVO uploadPhoto(Long userId, Long resumeId, MultipartFile file) throws IOException {
        validatePhoto(file);
        Resume resume = requireOwned(userId, resumeId);
        deletePhysicalPhoto(resume.getPhotoPath());

        FileStorageService.StoredFile stored = fileStorageService.store(file, PHOTO_SUBDIR);
        resume.setPhotoPath(stored.storedName());
        resumeMapper.updateById(resume);
        log.info("Resume photo uploaded resumeId={}, path={}", resumeId, stored.storedName());
        return resumeService.getResumeVo(resumeId, userId);
    }

    @Transactional
    public ResumeVO deletePhoto(Long userId, Long resumeId) {
        Resume resume = requireOwned(userId, resumeId);
        if (resume.getPhotoPath() == null || resume.getPhotoPath().isBlank()) {
            return resumeService.getResumeVo(resumeId, userId);
        }
        deletePhysicalPhoto(resume.getPhotoPath());
        resume.setPhotoPath(null);
        resumeMapper.updateById(resume);
        log.info("Resume photo deleted resumeId={}", resumeId);
        return resumeService.getResumeVo(resumeId, userId);
    }

    public PhotoPayload loadPhoto(Long userId, Long resumeId) throws IOException {
        Resume resume = requireOwned(userId, resumeId);
        if (resume.getPhotoPath() == null || resume.getPhotoPath().isBlank()) {
            throw new BusinessException(404, "该简历未上传照片");
        }
        var path = fileStorageService.resolve(resume.getPhotoPath());
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(404, "照片文件不存在");
        }
        byte[] bytes = Files.readAllBytes(path);
        return new PhotoPayload(bytes, guessMediaType(resume.getPhotoPath()));
    }

    public byte[] loadPhotoBytes(Resume resume) throws IOException {
        if (resume.getPhotoPath() == null || resume.getPhotoPath().isBlank()) {
            return null;
        }
        var path = fileStorageService.resolve(resume.getPhotoPath());
        if (!Files.isRegularFile(path)) {
            return null;
        }
        return Files.readAllBytes(path);
    }

    public void deletePhotoFile(String photoPath) {
        deletePhysicalPhoto(photoPath);
    }

    public static String photoUrl(Long resumeId, String photoPath) {
        if (resumeId == null || photoPath == null || photoPath.isBlank()) {
            return null;
        }
        return "/api/v1/resumes/" + resumeId + "/photo";
    }

    private Resume requireOwned(Long userId, Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BusinessException(404, "简历不存在");
        }
        return resume;
    }

    private void validatePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请上传照片文件");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw new BusinessException(400, "照片大小不能超过 2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(400, "仅支持 JPG、PNG 格式的一寸照");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".jpg") && !name.endsWith(".jpeg") && !name.endsWith(".png")) {
            throw new BusinessException(400, "仅支持 JPG、PNG 格式的一寸照");
        }
    }

    private void deletePhysicalPhoto(String photoPath) {
        if (photoPath == null || photoPath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(fileStorageService.resolve(photoPath));
        } catch (Exception e) {
            log.warn("删除照片失败: {}", photoPath);
        }
    }

    private static String guessMediaType(String photoPath) {
        String lower = photoPath.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        return MediaType.IMAGE_JPEG_VALUE;
    }

    public record PhotoPayload(byte[] bytes, String contentType) {
    }
}
