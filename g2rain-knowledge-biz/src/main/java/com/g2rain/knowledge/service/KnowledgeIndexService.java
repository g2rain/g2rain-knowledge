package com.g2rain.knowledge.service;

/** Writes a parsed revision to the staged Milvus projection. */
public interface KnowledgeIndexService {
    void index(Long revisionId);
}
