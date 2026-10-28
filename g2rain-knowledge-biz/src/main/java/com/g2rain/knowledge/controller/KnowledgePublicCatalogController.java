package com.g2rain.knowledge.controller;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.Result;
import com.g2rain.knowledge.service.KnowledgePublicCatalogService;
import com.g2rain.knowledge.vo.KnowledgeEvidenceVo;
import com.g2rain.knowledge.vo.KnowledgePublicCategoryVo;
import com.g2rain.knowledge.vo.KnowledgePublicItemVo;
import com.g2rain.web.interceptors.annotations.LoginGuard;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/knowledge/public")
public class KnowledgePublicCatalogController {
    @Resource
    private KnowledgePublicCatalogService knowledgePublicCatalogService;

    @GetMapping("/page")
    @LoginGuard
    @Operation(summary = "公共知识分页")
    public Result<PageData<KnowledgePublicItemVo>> page(
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "20") int pageSize) {
        return Result.successPage(knowledgePublicCatalogService.page(categoryId, keyword, pageNum, pageSize));
    }

    @GetMapping("/categories")
    @LoginGuard
    @Operation(summary = "公共知识分类导航")
    public Result<List<KnowledgePublicCategoryVo>> categories() {
        return Result.success(knowledgePublicCatalogService.categories());
    }

    @GetMapping("/{knowledgeId}")
    @LoginGuard
    @Operation(summary = "公共知识详情")
    public Result<KnowledgePublicItemVo> detail(@PathVariable Long knowledgeId) {
        return Result.success(knowledgePublicCatalogService.detail(knowledgeId));
    }

    @GetMapping("/{knowledgeId}/body")
    @LoginGuard
    @Operation(summary = "公共知识正文")
    public Result<KnowledgeEvidenceVo> body(@PathVariable Long knowledgeId) {
        return Result.success(knowledgePublicCatalogService.readBody(knowledgeId));
    }
}
