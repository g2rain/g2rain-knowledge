package com.g2rain.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "知识问答结果")
public class KnowledgeAnswerVo {
    private boolean answerable;
    private String traceId;
    private String reasonCode;
    private String answer;
    private List<KnowledgeEvidenceVo> sources;
}
