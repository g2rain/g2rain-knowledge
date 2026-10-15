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
 * 知识发布选择表返回VO
 * 关联表名: knowledge_release
 * 功能：封装接口返回数据，继承BaseVo复用基础字段逻辑，隔离数据库实体与前端展示层
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识发布选择表 VO")
public class KnowledgeReleaseVo extends BaseVo {

    /**
     * 机构标识
     */
    @Schema(description = "机构标识")
    private Long organId;

    /**
     * 知识主题标识
     */
    @Schema(description = "知识主题标识")
    private Long knowledgeId;

    /**
     * 当前生产生效版本标识
     */
    @Schema(description = "当前生产生效版本标识")
    private Long activeRevisionId;

    /**
     * 发布状态[UNPUBLISHED,PUBLISHED,SUSPENDED]
     */
    @Schema(description = "发布状态[UNPUBLISHED,PUBLISHED,SUSPENDED]")
    private String releaseStatus;

    /**
     * 最近发布用户标识
     */
    @Schema(description = "最近发布用户标识")
    private Long publishedUserId;

    /**
     * 最近发布时间
     */
    @Schema(description = "最近发布时间")
    private LocalDateTime publishedTime;

    /**
     * 发布或变更原因
     */
    @Schema(description = "发布或变更原因")
    private String releaseReason;

    /**
     * 删除标识[0:未删除, 1:已删除]
     */
    @Schema(description = "删除标识（0 未删除，1 已删除）", example = "false")
    @ConditionalJsonIgnore(adminCompany = AdminCompanyCondition.TRUE)
    private Boolean deleteFlag;
}