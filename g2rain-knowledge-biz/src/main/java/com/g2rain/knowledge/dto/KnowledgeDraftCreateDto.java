package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Creates an editable revision for an existing knowledge topic.
 */
@Getter
@Setter
@Schema(description = "创建知识草稿请求")
public class KnowledgeDraftCreateDto extends BaseDto {
    @NotNull
    private Long knowledgeId;

    @NotBlank
    @Size(max = 64)
    private String revisionNo;

    @NotBlank
    @Pattern(regexp = "FILE|FAQ")
    private String sourceType;
}
