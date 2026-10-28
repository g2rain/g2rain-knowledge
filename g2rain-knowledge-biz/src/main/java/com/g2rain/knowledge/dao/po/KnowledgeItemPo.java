package com.g2rain.knowledge.dao.po;

import com.g2rain.common.model.BasePo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * 知识主题表返回Po
 * 关联表名: knowledge_item
 * 功能：封装实体数据，继承BasePo复用基础字段逻辑
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KnowledgeItemPo extends BasePo {

    /**
     * 机构标识
     */
    private Long organId;

    /**
     * 知识分类标识
     */
    private Long categoryId;

    /**
     * 稳定业务键，如 refund_policy
     */
    private String knowledgeKey;

    /**
     * 知识名称
     */
    private String knowledgeName;

    /**
     * 知识类型[DOCUMENT:文档, FAQ:问答]
     */
    private String knowledgeType;

    /**
     * 状态[ACTIVE:有效, INACTIVE:停用]
     */
    private String status;

    /**
     * 知识说明
     */
    private String description;

    /**
     * 运营与同主题并列排序优先级，数值越大越靠前；不写入Milvus过滤
     */
    private Integer priority;

    /**
     * 主题已选标签编码数组，PROCESS/INDEX任务同步到Milvus Entity tags字段
     */
    private String tagsJson;

    /**
     * 共享范围[PRIVATE:仅本机构, PUBLIC:公共知识]
     */
    private String visibility;

    /**
     * 创建用户标识
     */
    private Long createUserId;

    /**
     * 删除标识[0:未删除, 1:已删除]
     */
    private Boolean deleteFlag;
}