package com.g2rain.knowledge.client.storage;

import java.io.InputStream;

/**
 * Controlled storage boundary for knowledge source files.
 */
public interface KnowledgeStorageClient {
    StoredSource storeSource(Long organId, Long knowledgeId, Long revisionId,
                             InputStream content, SourceMetadata metadata);

    void deleteUnreferenced(String relativePath);

    InputStream openSource(String relativePath);

    String writeParsed(Long organId, Long knowledgeId, Long revisionId, Long taskId, String content);

    String readParsedChunk(String relativePath, String locator);
}
