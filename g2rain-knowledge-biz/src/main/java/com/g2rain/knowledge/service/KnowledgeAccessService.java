package com.g2rain.knowledge.service;

/**
 * Resolves the trusted request identity used by knowledge-domain commands.
 */
public interface KnowledgeAccessService {
    Long requireOrganId();

    Long currentUserId();

    /**
     * Requires a non-null user id from the trusted principal (for createUserId etc.).
     */
    Long requireUserId();

    void assertSameOrgan(Long recordOrganId);
}
