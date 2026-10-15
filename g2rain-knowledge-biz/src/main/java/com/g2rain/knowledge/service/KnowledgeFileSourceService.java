package com.g2rain.knowledge.service;

import com.g2rain.knowledge.client.storage.SourceMetadata;

import java.io.InputStream;

/**
 * Coordinates file IO outside the transaction that attaches it to a draft.
 */
public interface KnowledgeFileSourceService {
    void replaceFile(Long revisionId, Integer expectedVersion, InputStream content, SourceMetadata metadata);
}
