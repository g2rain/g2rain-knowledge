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
 * 知识发布选择表查询DTO
 * 表名: knowledge_release
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识发布选择表 DTO")
public class KnowledgeReleaseDto extends BaseDto {

    /**
     * 机构标识
     */
    @NotNull(groups = CreateGroup.class)
    @Schema(description = "机构标识（新增必填）")
    private Long organId;

    /**
     * 知识主题标识
     */
    @NotNull(groups = CreateGroup.class)
    @Schema(description = "知识主题标识（新增必填）")
    private Long knowledgeId;

    /**
     * 当前生产生效版本标识
     */
    @Schema(description = "当前生产生效版本标识")
    private Long activeRevisionId;

    /**
     * 发布状态[UNPUBLISHED,PUBLISHED,SUSPENDED]
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 32)
    @Schema(description = "发布状态[UNPUBLISHED,PUBLISHED,SUSPENDED]（新增必填）", maxLength = 32)
    private String releaseStatus;

    /**
     * 最近发布用户标识
     */
    @Schema(description = "最近发布用户标识")
    private Long publishedUserId;

    /**
     * 最近发布时间
     */
    @Schema(description = "最近发布时间")
    private LocalDateTime publishedTime;

    /**
     * 发布或变更原因
     */
    @Size(max = 512)
    @Schema(description = "发布或变更原因", maxLength = 512)
    private String releaseReason;
}
