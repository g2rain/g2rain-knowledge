package com.g2rain.knowledge.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.g2rain.common.model.BaseSelectListDto;
import io.swagger.v3.oas.annotations.media.Schema;


/**
 * 知识回复模板表查询入参DTO
 * 用于KnowledgeAnswerTemplateDao.selectList方法的条件筛选
 * 表名: knowledge_answer_template
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识回复模板表查询入参 DTO")
public class KnowledgeAnswerTemplateSelectDto extends BaseSelectListDto {

    /**
     * 机构标识
     */
    @Schema(description = "机构标识")
    private Long organId;

    /**
     * 稳定模板编码，机构内唯一
     */
    @Schema(description = "稳定模板编码，机构内唯一")
    private String templateCode;

    /**
     * 模板展示名称
     */
    @Schema(description = "模板展示名称")
    private String templateName;

    /**
     * 状态[ACTIVE,INACTIVE]
     */
    @Schema(description = "状态[ACTIVE,INACTIVE]")
    private String status;

    /**
     * 管理备注
     */
    @Schema(description = "管理备注")
    private String remark;

    /**
     * 服务端回复模板内容
     */
    @Schema(description = "服务端回复模板内容")
    private String systemPrompt;
}