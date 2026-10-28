package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Shared options for knowledge batch import. Tenant is taken from the trusted context.
 */
@Getter
@Setter
@Schema(description = "知识批量导入选项")
public class KnowledgeImportOptionsDto extends BaseDto {

    @Schema(description = "分类标识")
    private Long categoryId;

    @Schema(description = "标签编码列表（写入主题；文件挂接导入忽略）")
    private List<String> tags;

    @Schema(description = "运营优先级（写入主题；省略默认 0；文件挂接导入忽略）")
    private Integer priority;

    @Schema(description = "导入成功后是否提交审核，默认 false（主题 CSV 不使用）")
    private Boolean submitReview;
}
