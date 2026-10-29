package com.g2rain.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate outcome of a knowledge batch import.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "知识批量导入结果")
public class KnowledgeImportResultVo {

    @Schema(description = "成功条数")
    private int successCount;

    @Schema(description = "跳过条数")
    private int skippedCount;

    @Schema(description = "失败条数")
    private int failedCount;

    @Schema(description = "明细列表")
    private List<KnowledgeImportItemVo> items = new ArrayList<>();
}
