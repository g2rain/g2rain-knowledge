package com.g2rain.knowledge.service;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.knowledge.dao.KnowledgeCategoryDao;
import com.g2rain.knowledge.dao.KnowledgeItemDao;
import com.g2rain.knowledge.dao.po.KnowledgeCategoryPo;
import com.g2rain.knowledge.dao.po.KnowledgeItemPo;
import com.g2rain.knowledge.domain.KnowledgeTagPolicy;
import com.g2rain.knowledge.dto.KnowledgeCategorySelectDto;
import com.g2rain.knowledge.enums.KnowledgeEnablementStatus;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.enums.KnowledgeSourceType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Shared create/save validations extracted from draft and CRUD save paths.
 */
@Component
public class KnowledgeSaveSupport {

    @Resource
    private KnowledgeCategoryDao knowledgeCategoryDao;
    @Resource
    private KnowledgeItemDao knowledgeItemDao;
    @Resource
    private KnowledgeTagPolicy knowledgeTagPolicy;
    @Resource
    private KnowledgeTagGovernanceService knowledgeTagGovernanceService;

    public void validateCategory(Long organId, Long categoryId) {
        if (Objects.isNull(categoryId)) {
            return;
        }
        KnowledgeCategoryPo category = knowledgeCategoryDao.selectById(categoryId);
        if (Objects.isNull(category) || !Objects.equals(category.getOrganId(), organId)
            || !KnowledgeEnablementStatus.ACTIVE.name().equals(category.getStatus())) {
            throw new BusinessException(KnowledgeErrorCode.CATEGORY_NOT_AVAILABLE);
        }
    }

    /**
     * Resolves an ACTIVE category by {@code category_code} within the organ.
     */
    public Long requireActiveCategoryIdByCode(Long organId, String categoryCode) {
        if (Objects.isNull(categoryCode) || categoryCode.isBlank()) {
            return null;
        }
        KnowledgeCategorySelectDto query = new KnowledgeCategorySelectDto();
        query.setOrganId(organId);
        query.setCategoryCode(categoryCode.trim());
        query.setStatus(KnowledgeEnablementStatus.ACTIVE.name());
        List<KnowledgeCategoryPo> found = knowledgeCategoryDao.selectList(query);
        if (found.isEmpty()) {
            throw new BusinessException(KnowledgeErrorCode.CATEGORY_NOT_AVAILABLE);
        }
        return found.getFirst().getId();
    }

    public KnowledgeItemPo requireActiveItem(Long knowledgeId, Long organId) {
        KnowledgeItemPo item = knowledgeItemDao.selectById(knowledgeId);
        if (Objects.isNull(item)) {
            throw new BusinessException(SystemErrorCode.DATA_NOT_EXISTS, knowledgeId);
        }
        if (!KnowledgeEnablementStatus.ACTIVE.name().equals(item.getStatus())) {
            throw new BusinessException(SystemErrorCode.PARAM_INVALID, "knowledgeId");
        }
        if (!Objects.equals(item.getOrganId(), organId)) {
            throw new BusinessException(SystemErrorCode.UNAUTHORIZED, "跨机构知识主题");
        }
        return item;
    }

    public void assertCompatibleSource(String knowledgeType, String sourceType) {
        boolean compatible = ("DOCUMENT".equals(knowledgeType) && KnowledgeSourceType.FILE.name().equals(sourceType))
            || ("FAQ".equals(knowledgeType) && KnowledgeSourceType.FAQ.name().equals(sourceType));
        if (!compatible) {
            throw new BusinessException(KnowledgeErrorCode.SOURCE_TYPE_MISMATCH);
        }
    }

    public String normalizeTagsJson(String tagsJson) {
        List<String> codes = knowledgeTagPolicy.parseTagsJson(tagsJson);
        return knowledgeTagPolicy.asJson(knowledgeTagGovernanceService.validateDraftTags(codes));
    }
}
