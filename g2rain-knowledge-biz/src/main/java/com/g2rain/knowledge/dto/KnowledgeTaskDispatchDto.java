package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Re-arms a PENDING task and republishes {@code KnowledgeTaskRequestedEvent}
 * so a worker can claim it again.
 */
@Getter
@Setter
@Schema(description = "推动 PENDING 知识处理任务执行")
public class KnowledgeTaskDispatchDto extends BaseDto {
    @NotNull
    private Long taskId;
}
