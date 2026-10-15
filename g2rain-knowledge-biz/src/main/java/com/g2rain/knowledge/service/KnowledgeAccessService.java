package com.g2rain.knowledge.service;

/**
 * Resolves the trusted request identity used by knowledge-domain commands.
 */
public interface KnowledgeAccessService {
    Long requireOrganId();

    Long currentUserId();

    void assertSameOrgan(Long recordOrganId);
}
