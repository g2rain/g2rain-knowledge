package com.g2rain.knowledge.controller;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.model.Result;
import com.g2rain.knowledge.api.KnowledgeCategoryApi;
import com.g2rain.knowledge.dto.KnowledgeCategoryDto;
import com.g2rain.knowledge.dto.KnowledgeCategorySelectDto;
import com.g2rain.knowledge.service.KnowledgeCategoryService;
import com.g2rain.knowledge.vo.KnowledgeCategoryVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import com.g2rain.web.interceptors.annotations.LoginGuard;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识分类表控制器
 * 表名: knowledge_category
 *
 * @author G2rain Generator
 */
@RestController
@RequestMapping("/knowledge_category")
public class KnowledgeCategoryController implements KnowledgeCategoryApi {

    @Resource(name = "knowledgeCategoryServiceImpl")
    private KnowledgeCategoryService knowledgeCategoryService;

    @Override
    public Result<List<KnowledgeCategoryVo>> selectList(KnowledgeCategorySelectDto selectDto) {
        return Result.success(knowledgeCategoryService.selectList(selectDto));
    }

    @Override
    public Result<PageData<KnowledgeCategoryVo>> selectPage(PageSelectListDto<KnowledgeCategorySelectDto> selectDto) {
        return Result.successPage(knowledgeCategoryService.selectPage(selectDto));
    }

    @PostMapping("/save")
    @LoginGuard
    @Operation(summary = "新增或更新知识分类表信息", description = "新增或更新知识分类表基础信息")
    public Result<Long> save(@RequestBody KnowledgeCategoryDto dto) {
        return Result.success(knowledgeCategoryService.save(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识分类表记录", description = "根据主键删除知识分类表记录")
    public Result<Integer> delete(@Parameter(description = "知识分类表标识") @PathVariable Long id) {
        return Result.success(knowledgeCategoryService.delete(id));
    }
}