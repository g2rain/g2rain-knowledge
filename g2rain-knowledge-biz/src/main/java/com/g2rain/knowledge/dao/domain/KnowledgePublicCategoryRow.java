package com.g2rain.knowledge.dao.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgePublicCategoryRow {
    private Long organId;
    private Long categoryId;
    private String categoryName;
    private long knowledgeCount;
}
