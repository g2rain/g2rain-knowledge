package com.g2rain.knowledge.client.storage;

/**
 * Metadata of a source persisted below the configured storage root.
 */
public record StoredSource(String relativePath, String displayFileName, String contentType,
                           String sha256, long size) {
}
