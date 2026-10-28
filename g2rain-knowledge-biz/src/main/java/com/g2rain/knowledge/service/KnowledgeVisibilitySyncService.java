package com.g2rain.knowledge.service;

/** Syncs MySQL theme visibility onto the active revision Milvus projection (zero re-embed). */
public interface KnowledgeVisibilitySyncService {
    void syncActiveRevision(Long knowledgeId);
}
