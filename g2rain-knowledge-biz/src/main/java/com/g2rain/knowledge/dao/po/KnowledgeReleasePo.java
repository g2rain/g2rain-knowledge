package com.g2rain.knowledge.dao.po;

import com.g2rain.common.model.BasePo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 知识发布选择表返回Po
 * 关联表名: knowledge_release
 * 功能：封装实体数据，继承BasePo复用基础字段逻辑
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KnowledgeReleasePo extends BasePo {

    /**
     * 机构标识
     */
    private Long organId;

    /**
     * 知识主题标识
     */
    private Long knowledgeId;

    /**
     * 当前生产生效版本标识
     */
    private Long activeRevisionId;

    /**
     * 发布状态[UNPUBLISHED,PUBLISHED,SUSPENDED]
     */
    private String releaseStatus;

    /**
     * 最近发布用户标识
     */
    private Long publishedUserId;

    /**
     * 最近发布时间
     */
    private LocalDateTime publishedTime;

    /**
     * 发布或变更原因
     */
    private String releaseReason;

    /**
     * 删除标识[0:未删除, 1:已删除]
     */
    private Boolean deleteFlag;
}