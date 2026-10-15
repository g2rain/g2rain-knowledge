package com.g2rain.knowledge.client.docling;

import com.g2rain.knowledge.client.storage.SourceMetadata;

import java.io.InputStream;

/**
 * Boundary for controlled document conversion. Callers receive normalized text
 * only and never a provider-specific response object.
 */
public interface DoclingClient {
    ParsedDocument parse(InputStream content, SourceMetadata metadata);
}
