package com.g2rain.knowledge.service.source;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Internal download handle for FILE revision sources. Not a public API DTO.
 */
public final class KnowledgeSourceDownload implements Closeable {
    private final String fileName;
    private final String contentType;
    private final InputStream content;

    public KnowledgeSourceDownload(String fileName, String contentType, InputStream content) {
        this.fileName = Objects.requireNonNullElse(fileName, "source.bin");
        this.contentType = Objects.requireNonNullElse(contentType, "application/octet-stream");
        this.content = Objects.requireNonNull(content, "content");
    }

    public String fileName() {
        return fileName;
    }

    public String contentType() {
        return contentType;
    }

    public InputStream content() {
        return content;
    }

    @Override
    public void close() throws IOException {
        content.close();
    }
}
