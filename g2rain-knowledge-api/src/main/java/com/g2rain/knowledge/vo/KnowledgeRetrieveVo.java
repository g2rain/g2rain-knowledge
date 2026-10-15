package com.g2rain.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Retrieval result; false means no valid active evidence was found. */
@Getter
@Setter
@Schema(description = "知识检索结果")
public class KnowledgeRetrieveVo {
    private boolean answerable;
    private String traceId;
    private String reasonCode;
    private List<KnowledgeEvidenceVo> sources;
}
