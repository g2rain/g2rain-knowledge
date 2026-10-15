package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalDate;
import com.g2rain.common.validation.CreateGroup;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 知识反馈表查询DTO
 * 表名: knowledge_feedback
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识反馈表 DTO")
public class KnowledgeFeedbackDto extends BaseDto {

    /**
     * 机构标识
     */
    @NotNull(groups = CreateGroup.class)
    @Schema(description = "机构标识（新增必填）")
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
    @Size(max = 64)
    @Schema(description = "Agent调用链标识", maxLength = 64)
    private String traceId;

    /**
     * 反馈类型[LOW_SCORE,WRONG_ANSWER,MANUAL_TAKEOVER,CORRECTED]
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 32)
    @Schema(description = "反馈类型[LOW_SCORE,WRONG_ANSWER,MANUAL_TAKEOVER,CORRECTED]（新增必填）", maxLength = 32)
    private String feedbackType;

    /**
     * 处理状态[OPEN,PROCESSING,RESOLVED,IGNORED]
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 32)
    @Schema(description = "处理状态[OPEN,PROCESSING,RESOLVED,IGNORED]（新增必填）", maxLength = 32)
    private String feedbackStatus;

    /**
     * 净化后的反馈摘要
     */
    @Size(max = 1024)
    @Schema(description = "净化后的反馈摘要", maxLength = 1024)
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
    @Size(max = 16777215)
    @Schema(description = "人工修正答案，受知识运营权限保护", maxLength = 16777215)
    private String correctedAnswer;
}
