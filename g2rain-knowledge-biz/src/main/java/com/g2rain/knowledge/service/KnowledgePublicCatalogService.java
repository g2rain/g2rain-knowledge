package com.g2rain.knowledge.service;

import com.g2rain.common.model.PageData;
import com.g2rain.knowledge.vo.KnowledgeEvidenceVo;
import com.g2rain.knowledge.vo.KnowledgePublicCategoryVo;
import com.g2rain.knowledge.vo.KnowledgePublicItemVo;

import java.util.List;

public interface KnowledgePublicCatalogService {
    PageData<KnowledgePublicItemVo> page(Long categoryId, String keyword, int pageNum, int pageSize);

    List<KnowledgePublicCategoryVo> categories();

    KnowledgePublicItemVo detail(Long knowledgeId);

    KnowledgeEvidenceVo readBody(Long knowledgeId);
}
