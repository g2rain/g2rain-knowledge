package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Cancels a task that has not been claimed by a worker.
 */
@Getter
@Setter
@Schema(description = "取消未开始的知识处理任务")
public class KnowledgeTaskCancelDto extends BaseDto {
    @NotNull
    private Long taskId;

    @NotNull
    private Integer expectedVersion;
}
