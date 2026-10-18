package com.g2rain.knowledge.controller;

import com.g2rain.common.model.Result;
import com.g2rain.knowledge.api.KnowledgeRuntimeApi;
import com.g2rain.knowledge.dto.KnowledgeAnswerDto;
import com.g2rain.knowledge.dto.KnowledgeRetrieveDto;
import com.g2rain.knowledge.service.KnowledgeAnswerService;
import com.g2rain.knowledge.service.KnowledgeRetrieveService;
import com.g2rain.knowledge.vo.KnowledgeAnswerVo;
import com.g2rain.knowledge.vo.KnowledgeRetrieveVo;
import com.g2rain.web.interceptors.annotations.LoginGuard;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeRuntimeController implements KnowledgeRuntimeApi {
    @Resource
    private KnowledgeRetrieveService knowledgeRetrieveService;
    @Resource
    private KnowledgeAnswerService knowledgeAnswerService;

    @Override
    @LoginGuard
    public Result<KnowledgeRetrieveVo> retrieve(@Valid @RequestBody KnowledgeRetrieveDto dto) {
        return Result.success(knowledgeRetrieveService.retrieve(dto));
    }

    @Override
    @LoginGuard
    public Result<KnowledgeAnswerVo> answer(@Valid @RequestBody KnowledgeAnswerDto dto) {
        return Result.success(knowledgeAnswerService.answer(dto));
    }
}
