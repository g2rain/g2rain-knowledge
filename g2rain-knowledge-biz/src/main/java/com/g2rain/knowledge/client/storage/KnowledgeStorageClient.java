package com.g2rain.knowledge.client.storage;

import com.g2rain.knowledge.domain.ChunkContent;

import java.io.InputStream;
import java.util.List;

/**
 * Controlled storage boundary for knowledge source files and parsed artifacts.
 */
public interface KnowledgeStorageClient {
    StoredSource storeSource(Long organId, Long knowledgeId, Long revisionId,
                             InputStream content, SourceMetadata metadata);

    void deleteUnreferenced(String relativePath);

    InputStream openSource(String relativePath);

    /**
     * Writes {@code document.md} and {@code chunks.json} under parsed/{taskId}/.
     * Returns the relative path of {@code document.md} (revision.parsed_storage_path).
     */
    String writeParsedArtifacts(Long organId, Long knowledgeId, Long revisionId, Long taskId,
                                String documentMarkdown, List<ChunkContent> chunks);

    /** Reads chunk raw text for evidence / Milvus content. Locator form: {@code chunk:{n}}. */
    String readParsedChunk(String relativePath, String locator);

    /** Reads embedding text (may include heading / FAQ context). */
    String readChunkForEmbedding(String relativePath, String locator);
}
