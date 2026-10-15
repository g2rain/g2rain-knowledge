package com.g2rain.knowledge.service;

import com.g2rain.knowledge.client.storage.StoredSource;

/**
 * Transaction boundary used after a source file has been safely stored.
 */
public interface KnowledgeDraftTxService {
    void attachFile(Long revisionId, Integer expectedVersion, StoredSource source);
}
