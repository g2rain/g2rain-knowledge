package com.g2rain.knowledge.controller;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.model.Result;
import com.g2rain.knowledge.api.KnowledgeTaskApi;
import com.g2rain.knowledge.dto.KnowledgeTaskCancelDto;
import com.g2rain.knowledge.dto.KnowledgeTaskDto;
import com.g2rain.knowledge.dto.KnowledgeTaskRetryDto;
import com.g2rain.knowledge.dto.KnowledgeTaskSelectDto;
import com.g2rain.knowledge.service.KnowledgeTaskCommandService;
import com.g2rain.knowledge.service.KnowledgeTaskService;
import com.g2rain.knowledge.vo.KnowledgeTaskVo;
import com.g2rain.web.interceptors.annotations.LoginGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识解析与索引处理任务表控制器
 * 表名: knowledge_task
 *
 * @author G2rain Generator
 */
@RestController
@RequestMapping("/knowledge_task")
public class KnowledgeTaskController implements KnowledgeTaskApi {

    @Resource(name = "knowledgeTaskServiceImpl")
    private KnowledgeTaskService knowledgeTaskService;
    @Resource
    private KnowledgeTaskCommandService knowledgeTaskCommandService;

    @Override
    public Result<List<KnowledgeTaskVo>> selectList(KnowledgeTaskSelectDto selectDto) {
        return Result.success(knowledgeTaskService.selectList(selectDto));
    }

    @Override
    public Result<PageData<KnowledgeTaskVo>> selectPage(PageSelectListDto<KnowledgeTaskSelectDto> selectDto) {
        return Result.successPage(knowledgeTaskService.selectPage(selectDto));
    }

    @PostMapping("/save")
    @LoginGuard
    @Operation(summary = "新增或更新知识解析与索引处理任务表信息", description = "新增或更新知识解析与索引处理任务表基础信息")
    public Result<Long> save(@RequestBody KnowledgeTaskDto dto) {
        return Result.success(knowledgeTaskService.save(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识解析与索引处理任务表记录", description = "根据主键删除知识解析与索引处理任务表记录")
    public Result<Integer> delete(@Parameter(description = "知识解析与索引处理任务表标识") @PathVariable Long id) {
        return Result.success(knowledgeTaskService.delete(id));
    }

    @Override
    @LoginGuard
    public Result<Long> processRevision(@PathVariable Long revisionId) {
        return Result.success(knowledgeTaskService.processRevision(revisionId));
    }

    @PostMapping("/retry")
    @LoginGuard
    @Operation(summary = "重试失败的处理任务", description = "仅 FAILED；保留原任务身份与幂等键")
    public Result<Void> retry(@Valid @RequestBody KnowledgeTaskRetryDto dto) {
        knowledgeTaskCommandService.retry(dto);
        return Result.success();
    }

    @PostMapping("/cancel")
    @LoginGuard
    @Operation(summary = "取消未开始的处理任务", description = "仅 PENDING；RUNNING 协作取消本轮未实现")
    public Result<Void> cancel(@Valid @RequestBody KnowledgeTaskCancelDto dto) {
        knowledgeTaskCommandService.cancel(dto);
        return Result.success();
    }
}
