package com.g2rain.knowledge.controller;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.model.Result;
import com.g2rain.knowledge.api.KnowledgeAnswerTemplateApi;
import com.g2rain.knowledge.dto.KnowledgeAnswerTemplateDto;
import com.g2rain.knowledge.dto.KnowledgeAnswerTemplateSelectDto;
import com.g2rain.knowledge.service.KnowledgeAnswerTemplateService;
import com.g2rain.knowledge.vo.KnowledgeAnswerTemplateVo;
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
 * 知识回复模板表控制器
 * 表名: knowledge_answer_template
 *
 * @author G2rain Generator
 */
@RestController
@RequestMapping("/knowledge_answer_template")
public class KnowledgeAnswerTemplateController implements KnowledgeAnswerTemplateApi {

    @Resource(name = "knowledgeAnswerTemplateServiceImpl")
    private KnowledgeAnswerTemplateService knowledgeAnswerTemplateService;

    @Override
    public Result<List<KnowledgeAnswerTemplateVo>> selectList(KnowledgeAnswerTemplateSelectDto selectDto) {
        return Result.success(knowledgeAnswerTemplateService.selectList(selectDto));
    }

    @Override
    public Result<PageData<KnowledgeAnswerTemplateVo>> selectPage(PageSelectListDto<KnowledgeAnswerTemplateSelectDto> selectDto) {
        return Result.successPage(knowledgeAnswerTemplateService.selectPage(selectDto));
    }

    @PostMapping("/save")
    @LoginGuard
    @Operation(summary = "新增或更新知识回复模板表信息", description = "新增或更新知识回复模板表基础信息")
    public Result<Long> save(@RequestBody KnowledgeAnswerTemplateDto dto) {
        return Result.success(knowledgeAnswerTemplateService.save(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识回复模板表记录", description = "根据主键删除知识回复模板表记录")
    public Result<Integer> delete(@Parameter(description = "知识回复模板表标识") @PathVariable Long id) {
        return Result.success(knowledgeAnswerTemplateService.delete(id));
    }
}