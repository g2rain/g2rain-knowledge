package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Switches one knowledge topic to an already indexed revision.
 * The tenant and publisher are always resolved from the trusted request context.
 * Concurrent updates still use DB-side version CAS; clients do not pass expected versions.
 */
@Getter
@Setter
@Schema(description = "发布知识版本请求")
public class KnowledgeReleasePublishDto extends BaseDto {
    @NotNull
    @Schema(description = "知识发布记录标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long releaseId;

    @NotNull
    @Schema(description = "待发布知识版本标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long revisionId;

    @Size(max = 512)
    @Schema(description = "发布原因", maxLength = 512)
    private String reason;
}
