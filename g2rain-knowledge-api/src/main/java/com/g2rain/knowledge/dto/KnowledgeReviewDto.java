package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Reviews a source revision. The actor is taken from the trusted context.
 */
@Getter
@Setter
@Schema(description = "审核知识版本请求")
public class KnowledgeReviewDto extends BaseDto {
    @NotNull
    private Long revisionId;

    @NotNull
    private Integer expectedVersion;

    @Size(max = 1024)
    private String comment;
}
