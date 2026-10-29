package com.g2rain.knowledge.dao.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgePublicCatalogRow {
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
