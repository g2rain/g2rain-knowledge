package com.g2rain.knowledge.controller;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.model.Result;
import com.g2rain.knowledge.api.KnowledgeReleaseApi;
import com.g2rain.knowledge.dto.KnowledgeReleaseDto;
import com.g2rain.knowledge.dto.KnowledgeReleasePublishDto;
import com.g2rain.knowledge.dto.KnowledgeReleaseRollbackDto;
import com.g2rain.knowledge.dto.KnowledgeReleaseSelectDto;
import com.g2rain.knowledge.service.KnowledgeReleaseService;
import com.g2rain.knowledge.vo.KnowledgeReleaseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.g2rain.web.interceptors.annotations.LoginGuard;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识发布选择表控制器
 * 表名: knowledge_release
 *
 * @author G2rain Generator
 */
@RestController
@RequestMapping("/knowledge_release")
public class KnowledgeReleaseController implements KnowledgeReleaseApi {

    @Resource(name = "knowledgeReleaseServiceImpl")
    private KnowledgeReleaseService knowledgeReleaseService;

    @Override
    public Result<List<KnowledgeReleaseVo>> selectList(KnowledgeReleaseSelectDto selectDto) {
        return Result.success(knowledgeReleaseService.selectList(selectDto));
    }

    @Override
    public Result<PageData<KnowledgeReleaseVo>> selectPage(PageSelectListDto<KnowledgeReleaseSelectDto> selectDto) {
        return Result.successPage(knowledgeReleaseService.selectPage(selectDto));
    }

    @PostMapping("/save")
    @Operation(summary = "新增或更新知识发布选择表信息", description = "新增或更新知识发布选择表基础信息")
    public Result<Long> save(@RequestBody KnowledgeReleaseDto dto) {
        return Result.success(knowledgeReleaseService.save(dto));
    }

    @PostMapping("/publish")
    @LoginGuard
    @Operation(summary = "发布知识版本", description = "将已完成索引的知识版本切换为主题的生产生效版本")
    public Result<Long> publish(@Valid @RequestBody KnowledgeReleasePublishDto dto) {
        return Result.success(knowledgeReleaseService.publish(dto));
    }

    @PostMapping("/rollback")
    @LoginGuard
    @Operation(summary = "回滚知识版本", description = "将已废弃且已完成索引的历史版本恢复为生产生效版本")
    public Result<Long> rollback(@Valid @RequestBody KnowledgeReleaseRollbackDto dto) {
        return Result.success(knowledgeReleaseService.rollback(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识发布选择表记录", description = "根据主键删除知识发布选择表记录")
    public Result<Integer> delete(@Parameter(description = "知识发布选择表标识") @PathVariable Long id) {
        return Result.success(knowledgeReleaseService.delete(id));
    }
}
