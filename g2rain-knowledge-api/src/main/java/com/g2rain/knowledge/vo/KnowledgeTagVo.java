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
 * 知识标签字典表返回VO
 * 关联表名: knowledge_tag
 * 功能：封装接口返回数据，继承BaseVo复用基础字段逻辑，隔离数据库实体与前端展示层
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识标签字典表 VO")
public class KnowledgeTagVo extends BaseVo {

    /**
     * 机构标识
     */
    @Schema(description = "机构标识")
    private Long organId;

    /**
     * 稳定业务编码，如 after_sales、x100；创建后冻结
     */
    @Schema(description = "稳定业务编码，如 after_sales、x100；创建后冻结")
    private String tagCode;

    /**
     * 展示名称，如 售后、X100
     */
    @Schema(description = "展示名称，如 售后、X100")
    private String tagName;

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