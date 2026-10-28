package com.g2rain.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "公共知识列表项")
public class KnowledgePublicItemVo {
    private Long knowledgeId;
    private Long organId;
    private Long categoryId;
    private String knowledgeKey;
    private String knowledgeName;
    private String knowledgeType;
    private String description;
    private String visibility;
    private Long activeRevisionId;
    private String revisionNo;
}
