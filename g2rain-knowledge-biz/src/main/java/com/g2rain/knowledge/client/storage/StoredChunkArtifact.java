package com.g2rain.knowledge.client.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One entry in parsed/chunks.json. MySQL keeps the manifest; this file holds bodies.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StoredChunkArtifact(
    int chunkNo,
    String sourceLocator,
    String rawText,
    String embeddingText,
    int tokenCount,
    Integer pageStart,
    Integer pageEnd,
    String titlePath,
    String contentSha256
) {
}
