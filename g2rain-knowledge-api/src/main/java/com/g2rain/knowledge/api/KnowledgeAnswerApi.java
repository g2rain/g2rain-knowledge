package com.g2rain.knowledge.api;

import com.g2rain.common.model.Result;
import com.g2rain.knowledge.dto.KnowledgeAnswerDto;
import com.g2rain.knowledge.vo.KnowledgeAnswerVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "知识问答", description = "基于已发布知识证据生成回复")
public interface KnowledgeAnswerApi {
    @PostMapping("/answer")
    @Operation(summary = "检索并生成知识回复", description = "仅将已通过活动版本复核的证据传给模型")
    Result<KnowledgeAnswerVo> answer(@Valid @RequestBody KnowledgeAnswerDto dto);
}
