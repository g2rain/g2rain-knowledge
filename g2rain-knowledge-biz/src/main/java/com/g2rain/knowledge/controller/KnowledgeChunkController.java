package com.g2rain.knowledge.controller;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.model.Result;
import com.g2rain.knowledge.api.KnowledgeChunkApi;
import com.g2rain.knowledge.dto.KnowledgeChunkDto;
import com.g2rain.knowledge.dto.KnowledgeChunkSelectDto;
import com.g2rain.knowledge.service.KnowledgeChunkService;
import com.g2rain.knowledge.vo.KnowledgeChunkVo;
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
 * 知识切片清单表控制器
 * 表名: knowledge_chunk
 *
 * @author G2rain Generator
 */
@RestController
@RequestMapping("/knowledge_chunk")
public class KnowledgeChunkController implements KnowledgeChunkApi {

    @Resource(name = "knowledgeChunkServiceImpl")
    private KnowledgeChunkService knowledgeChunkService;

    @Override
    public Result<List<KnowledgeChunkVo>> selectList(KnowledgeChunkSelectDto selectDto) {
        return Result.success(knowledgeChunkService.selectList(selectDto));
    }

    @Override
    public Result<PageData<KnowledgeChunkVo>> selectPage(PageSelectListDto<KnowledgeChunkSelectDto> selectDto) {
        return Result.successPage(knowledgeChunkService.selectPage(selectDto));
    }

    @PostMapping("/save")
    @LoginGuard
    @Operation(summary = "新增或更新知识切片清单表信息", description = "新增或更新知识切片清单表基础信息")
    public Result<Long> save(@RequestBody KnowledgeChunkDto dto) {
        return Result.success(knowledgeChunkService.save(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识切片清单表记录", description = "根据主键删除知识切片清单表记录")
    public Result<Integer> delete(@Parameter(description = "知识切片清单表标识") @PathVariable Long id) {
        return Result.success(knowledgeChunkService.delete(id));
    }
}