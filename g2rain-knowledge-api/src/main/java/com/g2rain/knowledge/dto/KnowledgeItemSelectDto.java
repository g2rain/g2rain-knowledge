package com.g2rain.knowledge.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.g2rain.common.model.BaseSelectListDto;
import io.swagger.v3.oas.annotations.media.Schema;


/**
 * 知识主题表查询入参DTO
 * 用于KnowledgeItemDao.selectList方法的条件筛选
 * 表名: knowledge_item
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识主题表查询入参 DTO")
public class KnowledgeItemSelectDto extends BaseSelectListDto {

    /**
     * 机构标识
     */
    @Schema(description = "机构标识")
    private Long organId;

    /**
     * 知识分类标识
     */
    @Schema(description = "知识分类标识")
    private Long categoryId;

    /**
     * 稳定业务键，如 refund_policy
     */
    @Schema(description = "稳定业务键，如 refund_policy")
    private String knowledgeKey;

    /**
     * 知识名称
     */
    @Schema(description = "知识名称")
    private String knowledgeName;

    /**
     * 知识类型[DOCUMENT:文档, FAQ:问答]
     */
    @Schema(description = "知识类型[DOCUMENT:文档, FAQ:问答]")
    private String knowledgeType;

    /**
     * 状态[ACTIVE:有效, INACTIVE:停用]
     */
    @Schema(description = "状态[ACTIVE:有效, INACTIVE:停用]")
    private String status;

    /**
     * 知识说明
     */
    @Schema(description = "知识说明")
    private String description;

    /**
     * 创建用户标识
     */
    @Schema(description = "创建用户标识")
    private Long createUserId;
}