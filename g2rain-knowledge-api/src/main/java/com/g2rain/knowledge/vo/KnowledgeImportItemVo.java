package com.g2rain.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row/file outcome of a knowledge batch import.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "知识批量导入明细")
public class KnowledgeImportItemVo {

    @Schema(description = "知识键")
    private String knowledgeKey;

    @Schema(description = "条目状态 SUCCESS / SKIPPED / FAILED")
    private String itemStatus;

    @Schema(description = "知识主题标识，成功时返回")
    private Long knowledgeId;

    @Schema(description = "知识版本标识，成功时返回")
    private Long revisionId;

    @Schema(description = "失败时的错误码")
    private String errorCode;

    @Schema(description = "失败或跳过说明")
    private String errorMessage;
}
