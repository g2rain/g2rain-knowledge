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
 * 知识分类表返回VO
 * 关联表名: knowledge_category
 * 功能：封装接口返回数据，继承BaseVo复用基础字段逻辑，隔离数据库实体与前端展示层
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识分类表 VO")
public class KnowledgeCategoryVo extends BaseVo {

    /**
     * 机构标识
     */
    @Schema(description = "机构标识")
    private Long organId;

    /**
     * 分类编码
     */
    @Schema(description = "分类编码")
    private String categoryCode;

    /**
     * 分类名称
     */
    @Schema(description = "分类名称")
    private String categoryName;

    /**
     * 状态[ACTIVE:有效, INACTIVE:停用]
     */
    @Schema(description = "状态[ACTIVE:有效, INACTIVE:停用]")
    private String status;

    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer sortOrder;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;

    /**
     * 删除标识[0:未删除, 1:已删除]
     */
    @Schema(description = "删除标识（0 未删除，1 已删除）", example = "false")
    @ConditionalJsonIgnore(adminCompany = AdminCompanyCondition.TRUE)
    private Boolean deleteFlag;
}