package com.g2rain.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/** One MySQL-verified retrieval evidence item. */
@Getter
@Setter
@Schema(description = "知识检索证据")
public class KnowledgeEvidenceVo {
    private Long knowledgeId;
    private Long revisionId;
    private Long chunkId;
    private String content;
    private String title;
    private String section;
    private Integer page;
    private String revisionNo;
    private String citation;
    private Float score;
}
