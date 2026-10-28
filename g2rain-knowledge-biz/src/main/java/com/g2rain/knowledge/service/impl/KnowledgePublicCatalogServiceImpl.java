package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.model.PageData;
import com.g2rain.knowledge.dao.domain.KnowledgePublicCatalogDomainDao;
import com.g2rain.knowledge.dao.domain.KnowledgePublicCatalogRow;
import com.g2rain.knowledge.dao.domain.KnowledgePublicCategoryRow;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgePublicCatalogService;
import com.g2rain.knowledge.service.KnowledgePublicEvidenceVerifier;
import com.g2rain.knowledge.vo.KnowledgeEvidenceVo;
import com.g2rain.knowledge.vo.KnowledgePublicCategoryVo;
import com.g2rain.knowledge.vo.KnowledgePublicItemVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgePublicCatalogServiceImpl implements KnowledgePublicCatalogService {
    private static final int MAX_PAGE_SIZE = 100;

    @Resource
    private KnowledgePublicCatalogDomainDao knowledgePublicCatalogDomainDao;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;
    @Resource
    private KnowledgePublicEvidenceVerifier knowledgePublicEvidenceVerifier;

    @Override
    public PageData<KnowledgePublicItemVo> page(Long categoryId, String keyword, int pageNum, int pageSize) {
        knowledgeAccessService.requireOrganId();
        int safePage = Math.max(pageNum, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        String safeKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        long total = knowledgePublicCatalogDomainDao.countPublic(categoryId, safeKeyword);
        int offset = (safePage - 1) * safeSize;
        List<KnowledgePublicItemVo> rows = knowledgePublicCatalogDomainDao
            .selectPublicPage(categoryId, safeKeyword, offset, safeSize)
            .stream()
            .map(this::toItem)
            .toList();
        return PageData.of(safePage, safeSize, total, rows);
    }

    @Override
    public List<KnowledgePublicCategoryVo> categories() {
        knowledgeAccessService.requireOrganId();
        return knowledgePublicCatalogDomainDao.selectPublicCategories().stream().map(this::toCategory).toList();
    }

    @Override
    public KnowledgePublicItemVo detail(Long knowledgeId) {
        Long organId = knowledgeAccessService.requireOrganId();
        KnowledgeEvidenceVo evidence = knowledgePublicEvidenceVerifier.verifyKnowledge(organId, true, knowledgeId)
            .orElseThrow(() -> new BusinessException(SystemErrorCode.DATA_NOT_EXISTS, knowledgeId));
        KnowledgePublicItemVo vo = new KnowledgePublicItemVo();
        vo.setKnowledgeId(evidence.getKnowledgeId());
        vo.setOrganId(evidence.getOrganId());
        vo.setCategoryId(evidence.getCategoryId());
        vo.setKnowledgeName(evidence.getTitle());
        vo.setVisibility(evidence.getVisibility());
        vo.setActiveRevisionId(evidence.getRevisionId());
        vo.setRevisionNo(evidence.getRevisionNo());
        vo.setDescription(null);
        return vo;
    }

    @Override
    public KnowledgeEvidenceVo readBody(Long knowledgeId) {
        Long organId = knowledgeAccessService.requireOrganId();
        return knowledgePublicEvidenceVerifier.verifyKnowledge(organId, true, knowledgeId)
            .orElseThrow(() -> new BusinessException(SystemErrorCode.DATA_NOT_EXISTS, knowledgeId));
    }

    private KnowledgePublicItemVo toItem(KnowledgePublicCatalogRow row) {
        KnowledgePublicItemVo vo = new KnowledgePublicItemVo();
        vo.setKnowledgeId(row.getKnowledgeId());
        vo.setOrganId(row.getOrganId());
        vo.setCategoryId(row.getCategoryId());
        vo.setKnowledgeKey(row.getKnowledgeKey());
        vo.setKnowledgeName(row.getKnowledgeName());
        vo.setKnowledgeType(row.getKnowledgeType());
        vo.setDescription(row.getDescription());
        vo.setVisibility(row.getVisibility());
        vo.setActiveRevisionId(row.getActiveRevisionId());
        vo.setRevisionNo(row.getRevisionNo());
        return vo;
    }

    private KnowledgePublicCategoryVo toCategory(KnowledgePublicCategoryRow row) {
        KnowledgePublicCategoryVo vo = new KnowledgePublicCategoryVo();
        vo.setOrganId(row.getOrganId());
        vo.setCategoryId(row.getCategoryId());
        vo.setCategoryName(row.getCategoryName());
        vo.setKnowledgeCount(row.getKnowledgeCount());
        return vo;
    }
}
