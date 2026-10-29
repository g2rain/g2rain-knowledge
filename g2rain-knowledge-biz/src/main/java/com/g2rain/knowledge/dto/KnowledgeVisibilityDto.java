package com.g2rain.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Dedicated command to change theme sharing scope (RQ-008). */
@Getter
@Setter
@Schema(description = "设置知识共享范围")
public class KnowledgeVisibilityDto {
    @NotNull
    @Schema(description = "主题 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long knowledgeId;

    @NotBlank
    @Size(max = 16)
    @Schema(description = "目标共享范围 PRIVATE/PUBLIC", requiredMode = Schema.RequiredMode.REQUIRED)
    private String visibility;

    @NotNull
    @Schema(description = "主题乐观锁版本", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer expectedVersion;
}
