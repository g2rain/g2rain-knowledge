package com.g2rain.knowledge.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.g2rain.common.model.BaseSelectListDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 知识反馈表查询入参DTO
 * 用于KnowledgeFeedbackDao.selectList方法的条件筛选
 * 表名: knowledge_feedback
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识反馈表查询入参 DTO")
public class KnowledgeFeedbackSelectDto extends BaseSelectListDto {

    /**
     * 机构标识
     */
    @Schema(description = "机构标识")
    private Long organId;

    /**
     * 关联知识版本标识
     */
    @Schema(description = "关联知识版本标识")
    private Long revisionId;

    /**
     * 关联知识切片标识
     */
    @Schema(description = "关联知识切片标识")
    private Long chunkId;

    /**
     * Agent调用链标识
     */
    @Schema(description = "Agent调用链标识")
    private String traceId;

    /**
     * 反馈类型[LOW_SCORE,WRONG_ANSWER,MANUAL_TAKEOVER,CORRECTED]
     */
    @Schema(description = "反馈类型[LOW_SCORE,WRONG_ANSWER,MANUAL_TAKEOVER,CORRECTED]")
    private String feedbackType;

    /**
     * 处理状态[OPEN,PROCESSING,RESOLVED,IGNORED]
     */
    @Schema(description = "处理状态[OPEN,PROCESSING,RESOLVED,IGNORED]")
    private String feedbackStatus;

    /**
     * 净化后的反馈摘要
     */
    @Schema(description = "净化后的反馈摘要")
    private String feedbackSummary;

    /**
     * 提交或处理用户标识
     */
    @Schema(description = "提交或处理用户标识")
    private Long operatorUserId;

    /**
     * 解决用户标识
     */
    @Schema(description = "解决用户标识")
    private Long resolvedUserId;

    /**
     * 解决时间
     */
    @Schema(description = "解决时间")
    private LocalDateTime resolvedTime;

    /**
     * 人工修正答案，受知识运营权限保护
     */
    @Schema(description = "人工修正答案，受知识运营权限保护")
    private String correctedAnswer;
}