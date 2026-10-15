package com.g2rain.knowledge.api;

import com.g2rain.common.model.Result;
import com.g2rain.knowledge.dto.KnowledgeRetrieveDto;
import com.g2rain.knowledge.vo.KnowledgeRetrieveVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "知识检索", description = "已发布知识的证据检索接口")
public interface KnowledgeRetrieveApi {
    @PostMapping("/retrieve")
    @Operation(summary = "检索知识证据", description = "仅返回通过组织和活动版本复核的证据")
    Result<KnowledgeRetrieveVo> retrieve(@Valid @RequestBody KnowledgeRetrieveDto dto);
}
