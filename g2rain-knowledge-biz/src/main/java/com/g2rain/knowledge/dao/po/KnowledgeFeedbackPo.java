package com.g2rain.knowledge.dao.po;

import com.g2rain.common.model.BasePo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 知识反馈表返回Po
 * 关联表名: knowledge_feedback
 * 功能：封装实体数据，继承BasePo复用基础字段逻辑
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KnowledgeFeedbackPo extends BasePo {

    /**
     * 机构标识
     */
    private Long organId;

    /**
     * 关联知识版本标识
     */
    private Long revisionId;

    /**
     * 关联知识切片标识
     */
    private Long chunkId;

    /**
     * Agent调用链标识
     */
    private String traceId;

    /**
     * 反馈类型[LOW_SCORE,WRONG_ANSWER,MANUAL_TAKEOVER,CORRECTED]
     */
    private String feedbackType;

    /**
     * 处理状态[OPEN,PROCESSING,RESOLVED,IGNORED]
     */
    private String feedbackStatus;

    /**
     * 净化后的反馈摘要
     */
    private String feedbackSummary;

    /**
     * 提交或处理用户标识
     */
    private Long operatorUserId;

    /**
     * 解决用户标识
     */
    private Long resolvedUserId;

    /**
     * 解决时间
     */
    private LocalDateTime resolvedTime;

    /**
     * 人工修正答案，受知识运营权限保护
     */
    private String correctedAnswer;

    /**
     * 删除标识[0:未删除, 1:已删除]
     */
    private Boolean deleteFlag;
}