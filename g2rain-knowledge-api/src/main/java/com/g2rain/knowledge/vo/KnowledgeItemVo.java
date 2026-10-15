package com.g2rain.knowledge.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.g2rain.common.model.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;

import com.g2rain.common.json.ConditionalJsonIgnore;
import com.g2rain.common.json.AdminCompanyCondition;

/**
 * 知识主题表返回VO
 * 关联表名: knowledge_item
 * 功能：封装接口返回数据，继承BaseVo复用基础字段逻辑，隔离数据库实体与前端展示层
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识主题表 VO")
public class KnowledgeItemVo extends BaseVo {

    /**
     * 机构标识
     */
    @Schema(description = "机构标识")
    private Long organId;

    /**
     * 知识分类标识
     */
    @Schema(description = "知识分类标识")
    private Long categoryId;

    /**
     * 稳定业务键，如 refund_policy
     */
    @Schema(description = "稳定业务键，如 refund_policy")
    private String knowledgeKey;

    /**
     * 知识名称
     */
    @Schema(description = "知识名称")
    private String knowledgeName;

    /**
     * 知识类型[DOCUMENT:文档, FAQ:问答]
     */
    @Schema(description = "知识类型[DOCUMENT:文档, FAQ:问答]")
    private String knowledgeType;

    /**
     * 状态[ACTIVE:有效, INACTIVE:停用]
     */
    @Schema(description = "状态[ACTIVE:有效, INACTIVE:停用]")
    private String status;

    /**
     * 知识说明
     */
    @Schema(description = "知识说明")
    private String description;

    /**
     * 创建用户标识
     */
    @Schema(description = "创建用户标识")
    private Long createUserId;

    /**
     * 删除标识[0:未删除, 1:已删除]
     */
    @Schema(description = "删除标识（0 未删除，1 已删除）", example = "false")
    @ConditionalJsonIgnore(adminCompany = AdminCompanyCondition.TRUE)
    private Boolean deleteFlag;
}