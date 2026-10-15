package com.g2rain.knowledge.service;

import com.g2rain.knowledge.dto.KnowledgeParseDto;

/**
 * Creates a durable parse task. Executing the task is handled separately.
 */
public interface KnowledgeParseService {
    Long submitParse(KnowledgeParseDto dto);
}
