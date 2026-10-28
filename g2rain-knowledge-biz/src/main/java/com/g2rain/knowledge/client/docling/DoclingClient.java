package com.g2rain.knowledge.client.docling;

import com.g2rain.knowledge.client.storage.SourceMetadata;
import com.g2rain.knowledge.domain.ChunkingProfile;
import com.g2rain.knowledge.domain.ChunkingResult;

import java.io.InputStream;

/**
 * Boundary for controlled document conversion and HybridChunker slicing.
 */
public interface DoclingClient {
    /** Legacy convert-only path retained for fixtures; PROCESS uses {@link #chunkFile}. */
    ParsedDocument parse(InputStream content, SourceMetadata metadata);

    /**
     * Structure-aware chunking via Docling Serve HybridChunker.
     *
     * @param fromFormat Serve {@code InputFormat}: {@code pdf}, {@code md}, or {@code html}
     */
    ChunkingResult chunkFile(InputStream content, SourceMetadata metadata, String fromFormat, ChunkingProfile profile);
}
