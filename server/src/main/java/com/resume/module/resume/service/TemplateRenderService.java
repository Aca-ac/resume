package com.resume.module.resume.service;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.data.PictureType;
import com.deepoove.poi.data.Pictures;
import com.deepoove.poi.data.Texts;
import com.resume.common.BusinessException;
import com.resume.module.resume.dto.TemplateRenderData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class TemplateRenderService {

    /** 一寸照渲染尺寸（约 3.2cm × 4.5cm @96dpi，较模板默认略大） */
    private static final int PHOTO_WIDTH_PX = 120;
    private static final int PHOTO_HEIGHT_PX = 168;

    public byte[] renderWord(String templateClasspathPath, TemplateRenderData data) throws IOException {
        if (templateClasspathPath == null || templateClasspathPath.isBlank()) {
            throw new BusinessException(400, "模板路径不能为空");
        }
        String normalized = normalizeClasspath(templateClasspathPath);
        ClassPathResource resource = new ClassPathResource(normalized);
        if (!resource.exists()) {
            throw new BusinessException(404, "模板文件不存在: " + templateClasspathPath);
        }

        boolean hasPhoto = data.getPhotoBytes() != null && data.getPhotoBytes().length > 0;
        Map<String, Object> renderMap = toRenderMap(data, hasPhoto);

        try (InputStream in = resource.getInputStream();
             XWPFTemplate template = XWPFTemplate.compile(in).render(renderMap);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            template.write(out);
            return DocxTemplateSanitizer.normalize(
                    out.toByteArray(), DocxTemplateSanitizer.textValuesFrom(data), hasPhoto);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Template render failed: {}", e.getMessage());
            throw new BusinessException(500, "模板渲染失败: " + e.getMessage());
        }
    }

    private Map<String, Object> toRenderMap(TemplateRenderData data, boolean hasPhoto) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", textField(data.getName()));
        map.put("phone", textField(data.getPhone()));
        map.put("email", textField(data.getEmail()));
        map.put("jobIntention", textField(data.getJobIntention()));
        map.put("education", textField(data.getEducation()));
        map.put("workExperience", textField(data.getWorkExperience()));
        map.put("project", textField(data.getProject()));
        map.put("skill", textField(data.getSkill()));
        map.put("summary", textField(data.getSummary()));
        if (hasPhoto) {
            map.put("photo", buildPhotoRender(data));
        } else {
            map.put("photo", null);
        }
        return map;
    }

    private Object textField(String value) {
        String text = safe(value);
        if (!text.contains("\n")) {
            return text;
        }
        return Texts.of(text).create();
    }

    private Object buildPhotoRender(TemplateRenderData data) {
        byte[] bytes = data.getPhotoBytes();
        if (bytes == null || bytes.length == 0) {
            return null;
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
