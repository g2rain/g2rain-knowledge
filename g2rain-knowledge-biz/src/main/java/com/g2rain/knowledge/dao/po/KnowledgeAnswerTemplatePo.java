package com.g2rain.knowledge.dao.po;

import com.g2rain.common.model.BasePo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * 知识回复模板表返回Po
 * 关联表名: knowledge_answer_template
 * 功能：封装实体数据，继承BasePo复用基础字段逻辑
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KnowledgeAnswerTemplatePo extends BasePo {

    /**
     * 机构标识
     */
    private Long organId;

    /**
     * 稳定模板编码，机构内唯一
     */
    private String templateCode;

    /**
     * 模板展示名称
     */
    private String templateName;

    /**
     * 状态[ACTIVE,INACTIVE]
     */
    private String status;

    /**
     * 管理备注
     */
    private String remark;

    /**
     * 服务端回复模板内容
     */
    private String systemPrompt;

    /**
     * 删除标识[0:未删除, 1:已删除]
     */
    private Boolean deleteFlag;
}