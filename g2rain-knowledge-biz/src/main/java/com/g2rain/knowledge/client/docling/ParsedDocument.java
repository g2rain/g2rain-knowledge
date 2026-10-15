package com.g2rain.knowledge.client.docling;

/**
 * The normalized textual output required by the knowledge parsing pipeline.
 */
public record ParsedDocument(String markdown, String text) {
}
