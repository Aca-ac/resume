package com.resume.module.resume.service;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.data.PictureType;
import com.deepoove.poi.data.Pictures;
import com.resume.common.BusinessException;
import com.resume.module.resume.dto.TemplateRenderData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class TemplateRenderService {

    /** 一寸照渲染尺寸（约 2.5cm × 3.5cm @96dpi） */
    private static final int PHOTO_WIDTH_PX = 95;
    private static final int PHOTO_HEIGHT_PX = 133;
    private static final byte[] TRANSPARENT_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    public byte[] renderWord(String templateClasspathPath, TemplateRenderData data) throws IOException {
        if (templateClasspathPath == null || templateClasspathPath.isBlank()) {
            throw new BusinessException(400, "模板路径不能为空");
        }
        String normalized = normalizeClasspath(templateClasspathPath);
        ClassPathResource resource = new ClassPathResource(normalized);
        if (!resource.exists()) {
            throw new BusinessException(404, "模板文件不存在: " + templateClasspathPath);
        }

        Map<String, Object> renderMap = toRenderMap(data);
        try (InputStream in = resource.getInputStream();
             XWPFTemplate template = XWPFTemplate.compile(in).render(renderMap);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            template.write(out);
            return out.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Template render failed: {}", e.getMessage());
            throw new BusinessException(500, "模板渲染失败: " + e.getMessage());
        }
    }

    private Map<String, Object> toRenderMap(TemplateRenderData data) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", safe(data.getName()));
        map.put("phone", safe(data.getPhone()));
        map.put("email", safe(data.getEmail()));
        map.put("education", safe(data.getEducation()));
        map.put("workExperience", safe(data.getWorkExperience()));
        map.put("project", safe(data.getProject()));
        map.put("skill", safe(data.getSkill()));
        map.put("summary", safe(data.getSummary()));
        map.put("photo", buildPhotoRender(data));
        return map;
    }

    private Object buildPhotoRender(TemplateRenderData data) {
        byte[] bytes = data.getPhotoBytes();
        if (bytes == null || bytes.length == 0) {
            return Pictures.ofBytes(TRANSPARENT_PNG, PictureType.PNG)
                    .size(1, 1)
                    .create();
        }
        PictureType type = pictureType(data.getPhotoExt());
        return Pictures.ofBytes(bytes, type)
                .size(PHOTO_WIDTH_PX, PHOTO_HEIGHT_PX)
                .create();
    }

    private PictureType pictureType(String ext) {
        if (ext != null && ext.toLowerCase(Locale.ROOT).endsWith("png")) {
            return PictureType.PNG;
        }
        return PictureType.JPEG;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalizeClasspath(String path) {
        String trimmed = path.trim();
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }
}
