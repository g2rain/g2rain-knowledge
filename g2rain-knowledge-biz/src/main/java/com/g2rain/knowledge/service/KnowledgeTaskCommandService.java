package com.g2rain.knowledge.service;

import com.g2rain.knowledge.dto.KnowledgeTaskCancelDto;
import com.g2rain.knowledge.dto.KnowledgeTaskRetryDto;

/**
 * Operator commands for existing processing tasks.
 */
public interface KnowledgeTaskCommandService {
    void retry(KnowledgeTaskRetryDto dto);

    void cancel(KnowledgeTaskCancelDto dto);
}
