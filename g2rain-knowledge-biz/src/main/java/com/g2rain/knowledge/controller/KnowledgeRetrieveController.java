package com.g2rain.knowledge.controller;

import com.g2rain.common.model.Result;
import com.g2rain.knowledge.api.KnowledgeRetrieveApi;
import com.g2rain.knowledge.dto.KnowledgeRetrieveDto;
import com.g2rain.knowledge.service.KnowledgeRetrieveService;
import com.g2rain.knowledge.vo.KnowledgeRetrieveVo;
import com.g2rain.web.interceptors.annotations.LoginGuard;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeRetrieveController implements KnowledgeRetrieveApi {
    @Resource
    private KnowledgeRetrieveService knowledgeRetrieveService;

    @Override
    @LoginGuard
    public Result<KnowledgeRetrieveVo> retrieve(@Valid @RequestBody KnowledgeRetrieveDto dto) {
        return Result.success(knowledgeRetrieveService.retrieve(dto));
    }
}
