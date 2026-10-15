package com.g2rain.knowledge.dao.po;

import com.g2rain.common.model.BasePo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * 知识分类表返回Po
 * 关联表名: knowledge_category
 * 功能：封装实体数据，继承BasePo复用基础字段逻辑
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KnowledgeCategoryPo extends BasePo {

    /**
     * 机构标识
     */
    private Long organId;

    /**
     * 分类编码
     */
    private String categoryCode;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 状态[ACTIVE:有效, INACTIVE:停用]
     */
    private String status;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标识[0:未删除, 1:已删除]
     */
    private Boolean deleteFlag;
}