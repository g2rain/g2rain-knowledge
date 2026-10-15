package com.g2rain.knowledge.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.g2rain.common.model.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;

import com.g2rain.common.json.ConditionalJsonIgnore;
import com.g2rain.common.json.AdminCompanyCondition;

/**
 * 知识回复模板表返回VO
 * 关联表名: knowledge_answer_template
 * 功能：封装接口返回数据，继承BaseVo复用基础字段逻辑，隔离数据库实体与前端展示层
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识回复模板表 VO")
public class KnowledgeAnswerTemplateVo extends BaseVo {

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

    /**
     * 删除标识[0:未删除, 1:已删除]
     */
    @Schema(description = "删除标识（0 未删除，1 已删除）", example = "false")
    @ConditionalJsonIgnore(adminCompany = AdminCompanyCondition.TRUE)
    private Boolean deleteFlag;
}