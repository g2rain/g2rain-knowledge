package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Client-facing semantic retrieval input. Tenant and index details are server-owned. */
@Getter
@Setter
@Schema(description = "知识检索请求")
public class KnowledgeRetrieveDto extends BaseDto {
    @NotBlank
    @Size(max = 2000)
    @Schema(description = "检索文本", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 2000)
    private String query;

    @Size(max = 64)
    @Schema(description = "必须同时命中的标签编码")
    private List<@Size(max = 64) String> allTagCodes;

    @Size(max = 64)
    @Schema(description = "至少命中一个的标签编码")
    private List<@Size(max = 64) String> anyTagCodes;

    @Schema(description = "期望返回的证据数，省略时使用服务默认值")
    private Integer topK;
}
