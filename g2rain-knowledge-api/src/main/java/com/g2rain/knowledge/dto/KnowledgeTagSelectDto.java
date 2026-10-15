package com.g2rain.knowledge.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.g2rain.common.model.BaseSelectListDto;
import io.swagger.v3.oas.annotations.media.Schema;


/**
 * 知识标签字典表查询入参DTO
 * 用于KnowledgeTagDao.selectList方法的条件筛选
 * 表名: knowledge_tag
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识标签字典表查询入参 DTO")
public class KnowledgeTagSelectDto extends BaseSelectListDto {

    /**
     * 机构标识
     */
    @Schema(description = "机构标识")
    private Long organId;

    /**
     * 稳定业务编码，如 after_sales、x100；创建后冻结
     */
    @Schema(description = "稳定业务编码，如 after_sales、x100；创建后冻结")
    private String tagCode;

    /**
     * 展示名称，如 售后、X100
     */
    @Schema(description = "展示名称，如 售后、X100")
    private String tagName;

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