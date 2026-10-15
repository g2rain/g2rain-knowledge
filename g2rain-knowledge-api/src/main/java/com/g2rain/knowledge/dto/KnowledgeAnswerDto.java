package com.g2rain.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/** Retrieval input plus a server-defined answer template selector. */
@Getter
@Setter
@Schema(description = "知识问答请求")
public class KnowledgeAnswerDto extends KnowledgeRetrieveDto {
    @Schema(description = "回复模板编码：DEFAULT、CONCISE、CUSTOMER_SERVICE", defaultValue = "DEFAULT")
    private String templateCode;
}
