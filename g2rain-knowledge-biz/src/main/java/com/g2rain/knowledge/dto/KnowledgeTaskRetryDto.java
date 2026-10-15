package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Retries the same failed processing task. The idempotency key and historical
 * attempt count are retained so a retry cannot create a competing task.
 */
@Getter
@Setter
@Schema(description = "重试失败的知识处理任务")
public class KnowledgeTaskRetryDto extends BaseDto {
    @NotNull
    private Long taskId;

    @NotNull
    private Integer expectedVersion;
}
