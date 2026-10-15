package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.g2rain.common.validation.CreateGroup;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 知识分类表查询DTO
 * 表名: knowledge_category
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识分类表 DTO")
public class KnowledgeCategoryDto extends BaseDto {

    /**
     * 机构标识
     */
    @NotNull(groups = CreateGroup.class)
    @Schema(description = "机构标识（新增必填）")
    private Long organId;

    /**
     * 分类编码
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 64)
    @Schema(description = "分类编码（新增必填）", maxLength = 64)
    private String categoryCode;

    /**
     * 分类名称
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 128)
    @Schema(description = "分类名称（新增必填）", maxLength = 128)
    private String categoryName;

    /**
     * 状态[ACTIVE:有效, INACTIVE:停用]
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 32)
    @Schema(description = "状态[ACTIVE:有效, INACTIVE:停用]（新增必填）", maxLength = 32)
    private String status;

    /**
     * 排序
     */
    @NotNull(groups = CreateGroup.class)
    @Schema(description = "排序（新增必填）")
    private Integer sortOrder;

    /**
     * 备注
     */
    @Size(max = 512)
    @Schema(description = "备注", maxLength = 512)
    private String remark;
}
