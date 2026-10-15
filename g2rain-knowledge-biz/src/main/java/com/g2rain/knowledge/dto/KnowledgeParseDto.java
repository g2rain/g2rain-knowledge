package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Requests parsing for an editable revision whose source has been attached.
 */
@Getter
@Setter
@Schema(description = "提交知识解析请求")
public class KnowledgeParseDto extends BaseDto {
    @NotNull
    private Long revisionId;

    @NotNull
    private Integer expectedVersion;
}
