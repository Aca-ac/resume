package com.resume.module.template.controller;

import com.resume.common.Result;
import com.resume.module.template.dto.TemplateRecommendRequest;
import com.resume.module.template.dto.TemplateRecommendVO;
import com.resume.module.template.service.TemplateRecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "简历模板推荐")
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateRecommendController {

    private final TemplateRecommendService recommendService;

    @Operation(summary = "按用户画像推荐模板")
    @PostMapping("/recommend")
    public Result<List<TemplateRecommendVO>> recommend(@RequestBody TemplateRecommendRequest request) {
        return Result.success(recommendService.recommend(request));
    }
}
