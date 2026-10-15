package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Updates the editable metadata of a draft revision.
 */
@Getter
@Setter
@Schema(description = "编辑知识草稿请求")
public class KnowledgeDraftEditDto extends BaseDto {
    @NotNull
    private Long revisionId;

    @NotNull
    private Integer expectedVersion;

    private List<@Size(max = 64) String> tags;

    private Integer priority;
}
