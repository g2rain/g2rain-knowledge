package com.g2rain.knowledge.enums;

/**
 * Lifecycle states persisted in {@code knowledge_revision}.
 */
public enum KnowledgeRevisionStatus {
    DRAFT,
    PARSING,
    PARSE_FAILED,
    REVIEWING,
    APPROVED,
    REJECTED,
    PROCESSING,
    PROCESS_FAILED,
    INDEXING,
    INDEX_FAILED,
    READY_TO_PUBLISH,
    PUBLISHED,
    DEPRECATED
}
