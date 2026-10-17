package com.g2rain.knowledge.client.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceFileKindTest {

    @Test
    void resolvesSupportedUploadKinds() {
        assertEquals(SourceFileKind.PDF, SourceFileKind.resolve("a.pdf", "application/pdf"));
        assertEquals(SourceFileKind.MARKDOWN, SourceFileKind.resolve("a.md", "text/markdown"));
        assertEquals(SourceFileKind.MARKDOWN, SourceFileKind.resolve("a.markdown", "text/plain"));
        assertEquals(SourceFileKind.HTML, SourceFileKind.resolve("a.html", "text/html"));
        assertEquals(SourceFileKind.HTML, SourceFileKind.resolve("a.htm", "application/xhtml+xml"));
        assertNull(SourceFileKind.resolve("a.txt", "text/plain"));
        assertNull(SourceFileKind.resolve("a.pdf", "text/plain"));
    }

    @Test
    void marksOnlyPdfAsDoclingRequired() {
        assertTrue(SourceFileKind.PDF.requiresDocling());
        assertFalse(SourceFileKind.MARKDOWN.requiresDocling());
        assertFalse(SourceFileKind.HTML.requiresDocling());
    }
}
