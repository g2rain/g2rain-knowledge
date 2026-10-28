package com.g2rain.knowledge.domain;

/**
 * Deterministic chunk for PROCESS: raw text is hashed and shown as evidence;
 * embedding text may include heading / FAQ question context.
 */
public record ChunkContent(
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
