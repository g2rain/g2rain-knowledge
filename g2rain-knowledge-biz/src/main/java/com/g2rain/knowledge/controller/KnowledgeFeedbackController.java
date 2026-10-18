package com.g2rain.knowledge.controller;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.model.Result;
import com.g2rain.knowledge.api.KnowledgeFeedbackApi;
import com.g2rain.knowledge.dto.KnowledgeFeedbackDto;
import com.g2rain.knowledge.dto.KnowledgeFeedbackSelectDto;
import com.g2rain.knowledge.service.KnowledgeFeedbackService;
import com.g2rain.knowledge.vo.KnowledgeFeedbackVo;
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
 * 知识反馈表控制器
 * 表名: knowledge_feedback
 *
 * @author G2rain Generator
 */
@RestController
@RequestMapping("/knowledge_feedback")
public class KnowledgeFeedbackController implements KnowledgeFeedbackApi {

    @Resource(name = "knowledgeFeedbackServiceImpl")
    private KnowledgeFeedbackService knowledgeFeedbackService;

    @Override
    public Result<List<KnowledgeFeedbackVo>> selectList(KnowledgeFeedbackSelectDto selectDto) {
        return Result.success(knowledgeFeedbackService.selectList(selectDto));
    }

    @Override
    public Result<PageData<KnowledgeFeedbackVo>> selectPage(PageSelectListDto<KnowledgeFeedbackSelectDto> selectDto) {
        return Result.successPage(knowledgeFeedbackService.selectPage(selectDto));
    }

    @PostMapping("/save")
    @LoginGuard
    @Operation(summary = "新增或更新知识反馈表信息", description = "新增或更新知识反馈表基础信息")
    public Result<Long> save(@RequestBody KnowledgeFeedbackDto dto) {
        return Result.success(knowledgeFeedbackService.save(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识反馈表记录", description = "根据主键删除知识反馈表记录")
    public Result<Integer> delete(@Parameter(description = "知识反馈表标识") @PathVariable Long id) {
        return Result.success(knowledgeFeedbackService.delete(id));
    }
}