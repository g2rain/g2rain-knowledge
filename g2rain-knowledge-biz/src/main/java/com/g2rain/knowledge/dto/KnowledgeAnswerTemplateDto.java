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
 * 知识回复模板表查询DTO
 * 表名: knowledge_answer_template
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识回复模板表 DTO")
public class KnowledgeAnswerTemplateDto extends BaseDto {

    /**
     * 机构标识（服务端填充）
     */
    @Schema(description = "机构标识（服务端填充，前端可省略）")
    private Long organId;

    /**
     * 稳定模板编码，机构内唯一
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 64)
    @Schema(description = "稳定模板编码，机构内唯一（新增必填）", maxLength = 64)
    private String templateCode;

    /**
     * 模板展示名称
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 128)
    @Schema(description = "模板展示名称（新增必填）", maxLength = 128)
    private String templateName;

    /**
     * 状态[ACTIVE,INACTIVE]；省略时服务端默认 ACTIVE
     */
    @Size(max = 32)
    @Schema(description = "状态[ACTIVE,INACTIVE]；省略时服务端默认 ACTIVE", maxLength = 32)
    private String status;

    /**
     * 管理备注
     */
    @Size(max = 512)
    @Schema(description = "管理备注", maxLength = 512)
    private String remark;

    /**
     * 服务端回复模板内容
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 16777215)
    @Schema(description = "服务端回复模板内容（新增必填）", maxLength = 16777215)
    private String systemPrompt;
}
