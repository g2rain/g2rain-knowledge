package com.g2rain.knowledge.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.g2rain.common.model.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.LocalDate;
import com.g2rain.common.json.ConditionalJsonIgnore;
import com.g2rain.common.json.AdminCompanyCondition;

/**
 * 知识解析与索引处理任务表返回VO
 * 关联表名: knowledge_task
 * 功能：封装接口返回数据，继承BaseVo复用基础字段逻辑，隔离数据库实体与前端展示层
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识解析与索引处理任务表 VO")
public class KnowledgeTaskVo extends BaseVo {

    /**
     * 乐观锁版本，供 retry / cancel 的 expectedVersion 使用
     */
    @Schema(description = "乐观锁版本，供 retry / cancel 的 expectedVersion 使用")
    private Integer version;

    /**
     * 机构标识
     */
    @Schema(description = "机构标识")
    private Long organId;

    /**
     * 知识版本标识
     */
    @Schema(description = "知识版本标识")
    private Long revisionId;

    /**
     * 知识处理任务类型[PARSE,INDEX,DELETE_INDEX]；不承载发布、回滚或下线
     */
    @Schema(description = "知识处理任务类型[PARSE,INDEX,DELETE_INDEX]；不承载发布、回滚或下线")
    private String taskType;

    /**
     * 任务状态[PENDING,RUNNING,SUCCESS,FAILED,CANCELED]
     */
    @Schema(description = "任务状态[PENDING,RUNNING,SUCCESS,FAILED,CANCELED]")
    private String taskStatus;

    /**
     * 幂等键
     */
    @Schema(description = "幂等键")
    private String idempotencyKey;

    /**
     * 尝试次数
     */
    @Schema(description = "尝试次数")
    private Integer attemptNo;

    /**
     * 任务可被领取时间，用于处理任务延迟重试
     */
    @Schema(description = "任务可被领取时间，用于处理任务延迟重试")
    private LocalDateTime availableTime;

    /**
     * 当前任务工作者标识
     */
    @Schema(description = "当前任务工作者标识")
    private String leaseOwner;

    /**
     * 任务领取租约到期时间
     */
    @Schema(description = "任务领取租约到期时间")
    private LocalDateTime leaseExpireTime;

    /**
     * 外部提供方名称
     */
    @Schema(description = "外部提供方名称")
    private String providerName;

    /**
     * 模型标识
     */
    @Schema(description = "模型标识")
    private String modelName;

    /**
     * 调用链标识
     */
    @Schema(description = "调用链标识")
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
    @Schema(description = "安全错误码")
    private String errorCode;

    /**
     * 净化后的错误摘要，不得含原文或Secret
     */
    @Schema(description = "净化后的错误摘要，不得含原文或Secret")
    private String errorSummary;

    /**
     * 删除标识[0:未删除, 1:已删除]
     */
    @Schema(description = "删除标识（0 未删除，1 已删除）", example = "false")
    @ConditionalJsonIgnore(adminCompany = AdminCompanyCondition.TRUE)
    private Boolean deleteFlag;
}