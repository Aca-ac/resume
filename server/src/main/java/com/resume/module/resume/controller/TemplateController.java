package com.resume.module.resume.controller;

import com.resume.common.Result;
import com.resume.module.resume.dto.PageResult;
import com.resume.module.resume.dto.TemplateRecommendVO;
import com.resume.module.resume.dto.TemplateSaveRequest;
import com.resume.module.resume.dto.TemplateVO;
import com.resume.module.resume.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * he：模板 CRUD + 推荐；渲染导出见 ResumeController（tian）。
 */
@Tag(name = "简历模板管理")
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @Operation(summary = "模板列表（支持分类筛选与分页）")
    @GetMapping
    public Result<PageResult<TemplateVO>> list(@RequestParam(required = false) String category,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return Result.success(templateService.list(category, page, size));
    }

    @Operation(summary = "智能推荐模板")
    @GetMapping("/recommend")
    public Result<List<TemplateRecommendVO>> recommend(@RequestParam(required = false) String education,
                                                       @RequestParam(required = false) String industry,
                                                       @RequestParam(required = false) String workYears,
                                                       @RequestParam(required = false) String targetPosition) {
        return Result.success(templateService.recommend(education, industry, workYears, targetPosition));
    }

    @Operation(summary = "模板详情")
    @GetMapping("/{id}")
    public Result<TemplateVO> detail(@PathVariable Long id) {
        return Result.success(templateService.get(id));
    }

    @Operation(summary = "新增模板")
    @PostMapping
    public Result<TemplateVO> create(@Valid @RequestBody TemplateSaveRequest request) {
        return Result.success(templateService.create(request));
    }

    @Operation(summary = "更新模板")
    @PutMapping("/{id}")
    public Result<TemplateVO> update(@PathVariable Long id,
                                     @Valid @RequestBody TemplateSaveRequest request) {
        return Result.success(templateService.update(id, request));
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return Result.success();
    }
}
