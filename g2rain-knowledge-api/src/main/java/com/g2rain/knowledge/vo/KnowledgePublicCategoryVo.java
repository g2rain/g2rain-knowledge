package com.g2rain.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "公共知识分类导航项")
public class KnowledgePublicCategoryVo {
    private Long organId;
    private Long categoryId;
    private String categoryName;
    private long knowledgeCount;
}
