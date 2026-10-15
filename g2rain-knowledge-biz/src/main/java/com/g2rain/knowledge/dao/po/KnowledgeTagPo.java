package com.g2rain.knowledge.dao.po;

import com.g2rain.common.model.BasePo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * 知识标签字典表返回Po
 * 关联表名: knowledge_tag
 * 功能：封装实体数据，继承BasePo复用基础字段逻辑
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KnowledgeTagPo extends BasePo {

    /**
     * 机构标识
     */
    private Long organId;

    /**
     * 稳定业务编码，如 after_sales、x100；创建后冻结
     */
    private String tagCode;

    /**
     * 展示名称，如 售后、X100
     */
    private String tagName;

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