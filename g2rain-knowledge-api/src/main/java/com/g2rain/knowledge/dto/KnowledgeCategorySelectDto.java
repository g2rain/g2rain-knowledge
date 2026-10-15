package com.g2rain.knowledge.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.g2rain.common.model.BaseSelectListDto;
import io.swagger.v3.oas.annotations.media.Schema;


/**
 * 知识分类表查询入参DTO
 * 用于KnowledgeCategoryDao.selectList方法的条件筛选
 * 表名: knowledge_category
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识分类表查询入参 DTO")
public class KnowledgeCategorySelectDto extends BaseSelectListDto {

    /**
     * 机构标识
     */
    @Schema(description = "机构标识")
    private Long organId;

    /**
     * 分类编码
     */
    @Schema(description = "分类编码")
    private String categoryCode;

    /**
     * 分类名称
     */
    @Schema(description = "分类名称")
    private String categoryName;

    /**
     * 状态[ACTIVE:有效, INACTIVE:停用]
     */
    @Schema(description = "状态[ACTIVE:有效, INACTIVE:停用]")
    private String status;

    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer sortOrder;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;
}