package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalDate;
import com.g2rain.common.validation.CreateGroup;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 知识解析与索引处理任务表查询DTO
 * 表名: knowledge_task
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识解析与索引处理任务表 DTO")
public class KnowledgeTaskDto extends BaseDto {

    /**
     * 机构标识
     */
    @NotNull(groups = CreateGroup.class)
    @Schema(description = "机构标识（新增必填）")
    private Long organId;

    /**
     * 知识版本标识
     */
    @NotNull(groups = CreateGroup.class)
    @Schema(description = "知识版本标识（新增必填）")
    private Long revisionId;

    /**
     * 知识处理任务类型[PARSE,INDEX,DELETE_INDEX]；不承载发布、回滚或下线
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 32)
    @Schema(description = "知识处理任务类型[PARSE,INDEX,DELETE_INDEX]；不承载发布、回滚或下线（新增必填）", maxLength = 32)
    private String taskType;

    /**
     * 任务状态[PENDING,RUNNING,SUCCESS,FAILED,CANCELED]
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 32)
    @Schema(description = "任务状态[PENDING,RUNNING,SUCCESS,FAILED,CANCELED]（新增必填）", maxLength = 32)
    private String taskStatus;

    /**
     * 幂等键
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 128)
    @Schema(description = "幂等键（新增必填）", maxLength = 128)
    private String idempotencyKey;

    /**
     * 尝试次数
     */
    @NotNull(groups = CreateGroup.class)
    @Schema(description = "尝试次数（新增必填）")
    private Integer attemptNo;

    /**
     * 任务可被领取时间，用于处理任务延迟重试
     */
    @NotNull(groups = CreateGroup.class)
    @Schema(description = "任务可被领取时间，用于处理任务延迟重试（新增必填）")
    private LocalDateTime availableTime;

    /**
     * 当前任务工作者标识
     */
    @Size(max = 128)
    @Schema(description = "当前任务工作者标识", maxLength = 128)
    private String leaseOwner;

    /**
     * 任务领取租约到期时间
     */
    @Schema(description = "任务领取租约到期时间")
    private LocalDateTime leaseExpireTime;

    /**
     * 外部提供方名称
     */
    @Size(max = 64)
    @Schema(description = "外部提供方名称", maxLength = 64)
    private String providerName;

    /**
     * 模型标识
     */
    @Size(max = 128)
    @Schema(description = "模型标识", maxLength = 128)
    private String modelName;

    /**
     * 调用链标识
     */
    @Size(max = 64)
    @Schema(description = "调用链标识", maxLength = 64)
    private String traceId;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间")
    private LocalDateTime startedTime;

    /**
     * 完成时间
     */
    @Schema(description = "完成时间")
    private LocalDateTime finishedTime;

    /**
     * 安全错误码
     */
    @Size(max = 64)
    @Schema(description = "安全错误码", maxLength = 64)
    private String errorCode;

    /**
     * 净化后的错误摘要，不得含原文或Secret
     */
    @Size(max = 1024)
    @Schema(description = "净化后的错误摘要，不得含原文或Secret", maxLength = 1024)
    private String errorSummary;
}
