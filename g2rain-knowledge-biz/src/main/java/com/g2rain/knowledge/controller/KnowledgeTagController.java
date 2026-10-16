package com.g2rain.knowledge.controller;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.model.Result;
import com.g2rain.knowledge.api.KnowledgeTagApi;
import com.g2rain.knowledge.dto.KnowledgeTagDto;
import com.g2rain.knowledge.dto.KnowledgeTagSelectDto;
import com.g2rain.knowledge.service.KnowledgeTagService;
import com.g2rain.knowledge.vo.KnowledgeTagVo;
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
 * 知识标签字典表控制器
 * 表名: knowledge_tag
 *
 * @author G2rain Generator
 */
@RestController
@RequestMapping("/knowledge_tag")
public class KnowledgeTagController implements KnowledgeTagApi {

    @Resource(name = "knowledgeTagServiceImpl")
    private KnowledgeTagService knowledgeTagService;

    @Override
    public Result<List<KnowledgeTagVo>> selectList(KnowledgeTagSelectDto selectDto) {
        return Result.success(knowledgeTagService.selectList(selectDto));
    }

    @Override
    public Result<PageData<KnowledgeTagVo>> selectPage(PageSelectListDto<KnowledgeTagSelectDto> selectDto) {
        return Result.successPage(knowledgeTagService.selectPage(selectDto));
    }

    @PostMapping("/save")
    @LoginGuard
    @Operation(summary = "新增或更新知识标签字典表信息", description = "新增或更新知识标签字典表基础信息")
    public Result<Long> save(@RequestBody KnowledgeTagDto dto) {
        return Result.success(knowledgeTagService.save(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识标签字典表记录", description = "根据主键删除知识标签字典表记录")
    public Result<Integer> delete(@Parameter(description = "知识标签字典表标识") @PathVariable Long id) {
        return Result.success(knowledgeTagService.delete(id));
    }
}