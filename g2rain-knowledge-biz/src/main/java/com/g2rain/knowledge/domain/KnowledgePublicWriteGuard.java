package com.g2rain.knowledge.domain;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.enums.KnowledgeVisibility;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * RQ-008: any write that touches PUBLIC knowledge requires trusted adminCompany.
 */
@Component
public class KnowledgePublicWriteGuard {
    @Resource
    private KnowledgeAccessService knowledgeAccessService;

    public void requireIfPublic(String... visibilityValues) {
        for (String value : visibilityValues) {
            if (KnowledgeVisibility.isPublic(value)) {
                knowledgeAccessService.requireAdminCompany();
                return;
            }
        }
    }

    public KnowledgeVisibility requireValid(String raw) {
        try {
            KnowledgeVisibility visibility = KnowledgeVisibility.parse(raw);
            if (raw != null && !raw.isBlank()
                && !visibility.name().equalsIgnoreCase(raw.trim())) {
                throw new BusinessException(KnowledgeErrorCode.INVALID_VISIBILITY);
            }
            return visibility;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(KnowledgeErrorCode.INVALID_VISIBILITY);
        }
    }
}
