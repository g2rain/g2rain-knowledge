package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Updates the structured source of a FAQ draft.
 */
@Getter
@Setter
@Schema(description = "编辑 FAQ 草稿请求")
public class KnowledgeFaqEditDto extends BaseDto {
    @NotNull
    private Long revisionId;

    @NotNull
    private Integer expectedVersion;

    @NotBlank
    @Size(max = 65535)
    private String question;

    @NotBlank
    @Size(max = 16777215)
    private String answer;
}
