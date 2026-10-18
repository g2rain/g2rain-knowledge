package com.g2rain.knowledge.api;

import com.g2rain.common.model.Result;
import com.g2rain.knowledge.dto.KnowledgeAnswerDto;
import com.g2rain.knowledge.dto.KnowledgeRetrieveDto;
import com.g2rain.knowledge.vo.KnowledgeAnswerVo;
import com.g2rain.knowledge.vo.KnowledgeRetrieveVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "知识运行时", description = "已发布知识的证据检索与问答接口")
public interface KnowledgeRuntimeApi {
    @PostMapping("/retrieve")
    @Operation(summary = "检索知识证据", description = "仅返回通过组织和活动版本复核的证据")
    Result<KnowledgeRetrieveVo> retrieve(@Valid @RequestBody KnowledgeRetrieveDto dto);

    @PostMapping("/answer")
    @Operation(summary = "检索并生成知识回复", description = "仅将已通过活动版本复核的证据传给模型")
    Result<KnowledgeAnswerVo> answer(@Valid @RequestBody KnowledgeAnswerDto dto);
}
