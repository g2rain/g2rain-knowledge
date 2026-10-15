package com.g2rain.knowledge.domain;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.enums.KnowledgeRevisionStatus;
import org.springframework.stereotype.Component;

/**
 * Pure lifecycle rules for mutable revision fields.
 */
@Component
public class KnowledgeRevisionStateMachine {
    public void requireEditable(String status) {
        if (!KnowledgeRevisionStatus.DRAFT.name().equals(status)) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_NOT_EDITABLE);
        }
    }
}
