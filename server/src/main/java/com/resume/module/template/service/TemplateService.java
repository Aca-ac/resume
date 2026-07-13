package com.resume.module.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.BusinessException;
import com.resume.module.resume.dto.PageResult;
import com.resume.module.template.dto.TemplateCreateRequest;
import com.resume.module.template.dto.TemplateUploadVO;
import com.resume.module.template.dto.TemplateVO;
import com.resume.module.template.entity.Template;
import com.resume.module.template.mapper.TemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TemplateMapper templateMapper;
    private final TemplateFileStorageService fileStorage;

    @Transactional
    public TemplateVO create(TemplateCreateRequest req, MultipartFile docx, MultipartFile preview) throws IOException {
        String templatePath = req.getTemplatePath();
        String previewUrl = req.getPreviewUrl();
        if (docx != null && !docx.isEmpty()) {
            templatePath = fileStorage.storeTemplateDocx(docx).getPath();
        }
        if (preview != null && !preview.isEmpty()) {
            previewUrl = fileStorage.storePreview(preview).getPath();
        }
        if (templatePath == null || templatePath.isBlank()) {
            throw new BusinessException(400, "请上传 docx 模板或提供 templatePath");
        }
        if (!fileStorage.resolveTemplatePath(templatePath).toFile().exists()) {
            throw new BusinessException(400, "模板文件不存在: " + templatePath);
        }

        Template entity = new Template();
        entity.setName(req.getName().trim());
        entity.setCategory(blankToDefault(req.getCategory(), "通用"));
        entity.setApplicableScene(req.getApplicableScene());
        entity.setTemplatePath(templatePath);
        entity.setPreviewUrl(previewUrl);
        templateMapper.insert(entity);
        log.info("Created template id={}, name={}", entity.getId(), entity.getName());
        return toVO(entity);
    }

    public PageResult<TemplateVO> list(String category, int page, int size) {
        LambdaQueryWrapper<Template> q = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank()) {
            q.eq(Template::getCategory, category.trim());
        }
        q.orderByDesc(Template::getUpdatedAt);
        Page<Template> result = templateMapper.selectPage(new Page<>(page, size), q);
        List<TemplateVO> records = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, result.getTotal(), page, size);
    }

    public TemplateVO get(Long id) {
        return toVO(require(id));
    }

    public Template require(Long id) {
        Template t = templateMapper.selectById(id);
        if (t == null) {
            throw new BusinessException(404, "模板不存在");
        }
        return t;
    }

    @Transactional
    public void delete(Long id) {
        Template t = require(id);
        fileStorage.deleteTemplateRelative(t.getTemplatePath());
        fileStorage.deleteTemplateRelative(t.getPreviewUrl());
        templateMapper.deleteById(id);
        log.info("Deleted template id={}", id);
    }

    public TemplateUploadVO upload(String type, MultipartFile file) throws IOException {
        if ("preview".equalsIgnoreCase(type)) {
            return fileStorage.storePreview(file);
        }
        return fileStorage.storeTemplateDocx(file);
    }

    public List<Template> listAll() {
        return templateMapper.selectList(new LambdaQueryWrapper<Template>().orderByDesc(Template::getUpdatedAt));
    }

    public TemplateVO toVO(Template t) {
        TemplateVO vo = new TemplateVO();
        vo.setId(t.getId());
        vo.setName(t.getName());
        vo.setCategory(t.getCategory());
        vo.setPreviewUrl(t.getPreviewUrl());
        vo.setTemplatePath(t.getTemplatePath());
        vo.setApplicableScene(t.getApplicableScene());
        vo.setCreatedAt(t.getCreatedAt() == null ? null : t.getCreatedAt().format(FMT));
        vo.setUpdatedAt(t.getUpdatedAt() == null ? null : t.getUpdatedAt().format(FMT));
        return vo;
    }

    private static String blankToDefault(String v, String def) {
        return v == null || v.isBlank() ? def : v.trim();
    }
}
