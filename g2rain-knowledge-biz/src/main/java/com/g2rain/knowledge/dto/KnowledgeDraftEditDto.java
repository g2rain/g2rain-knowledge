package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Touches a draft revision (optimistic lock). Tags and priority live on the knowledge item.
 */
@Getter
@Setter
@Schema(description = "编辑知识草稿请求")
public class KnowledgeDraftEditDto extends BaseDto {
    @NotNull
    private Long revisionId;

    @NotNull
    private Integer expectedVersion;
}
