package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.g2rain.common.validation.CreateGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 知识标签字典表查询DTO
 * 表名: knowledge_tag
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识标签字典表 DTO")
public class KnowledgeTagDto extends BaseDto {

    /**
     * 机构标识（服务端填充）
     */
    @Schema(description = "机构标识（服务端填充，前端可省略）")
    private Long organId;

    /**
     * 稳定业务编码，如 after_sales、x100；创建后冻结
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 64)
    @Schema(description = "稳定业务编码，如 after_sales、x100；创建后冻结（新增必填）", maxLength = 64)
    private String tagCode;

    /**
     * 展示名称，如 售后、X100
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 128)
    @Schema(description = "展示名称，如 售后、X100（新增必填）", maxLength = 128)
    private String tagName;

    /**
     * 状态[ACTIVE:有效, INACTIVE:停用]；省略时服务端默认 ACTIVE
     */
    @Size(max = 32)
    @Schema(description = "状态[ACTIVE:有效, INACTIVE:停用]；省略时服务端默认 ACTIVE", maxLength = 32)
    private String status;

    /**
     * 排序；省略时服务端默认 0
     */
    @Schema(description = "排序；省略时服务端默认 0")
    private Integer sortOrder;

    /**
     * 备注
     */
    @Size(max = 512)
    @Schema(description = "备注", maxLength = 512)
    private String remark;
}
