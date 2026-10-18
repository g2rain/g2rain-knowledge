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
 * 知识主题表查询DTO
 * 表名: knowledge_item
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识主题表 DTO")
public class KnowledgeItemDto extends BaseDto {

    /**
     * 机构标识（服务端填充）
     */
    @Schema(description = "机构标识（服务端填充，前端可省略）")
    private Long organId;

    /**
     * 知识分类标识
     */
    @Schema(description = "知识分类标识")
    private Long categoryId;

    /**
     * 稳定业务键，如 refund_policy
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 128)
    @Schema(description = "稳定业务键，如 refund_policy（新增必填）", maxLength = 128)
    private String knowledgeKey;

    /**
     * 知识名称
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 256)
    @Schema(description = "知识名称（新增必填）", maxLength = 256)
    private String knowledgeName;

    /**
     * 知识类型[DOCUMENT:文档, FAQ:问答]
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 32)
    @Schema(description = "知识类型[DOCUMENT:文档, FAQ:问答]（新增必填）", maxLength = 32)
    private String knowledgeType;

    /**
     * 状态[ACTIVE:有效, INACTIVE:停用]；省略时服务端默认 ACTIVE
     */
    @Size(max = 32)
    @Schema(description = "状态[ACTIVE:有效, INACTIVE:停用]；省略时服务端默认 ACTIVE", maxLength = 32)
    private String status;

    /**
     * 知识说明
     */
    @Size(max = 1024)
    @Schema(description = "知识说明", maxLength = 1024)
    private String description;

    /**
     * 创建用户标识（服务端填充）
     */
    @Schema(description = "创建用户标识（服务端填充，前端可省略）")
    private Long createUserId;
}
