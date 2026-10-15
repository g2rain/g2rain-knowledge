package com.g2rain.knowledge.client.storage;

/**
 * Untrusted multipart metadata supplied with a source file.
 */
public record SourceMetadata(String originalFileName, String contentType, long declaredSize) {
}
