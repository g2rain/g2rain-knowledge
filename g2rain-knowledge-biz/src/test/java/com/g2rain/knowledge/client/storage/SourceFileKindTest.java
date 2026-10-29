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
        assertEquals(SourceFileKind.DOCX, SourceFileKind.resolve("a.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        assertEquals(SourceFileKind.DOC, SourceFileKind.resolve("a.doc", "application/msword"));
        assertEquals(SourceFileKind.PPTX, SourceFileKind.resolve("a.pptx",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        assertEquals(SourceFileKind.PPT, SourceFileKind.resolve("a.ppt", "application/vnd.ms-powerpoint"));
        assertEquals(SourceFileKind.XLSX, SourceFileKind.resolve("a.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        assertEquals(SourceFileKind.XLS, SourceFileKind.resolve("a.xls", "application/vnd.ms-excel"));
        assertEquals(SourceFileKind.MARKDOWN, SourceFileKind.resolve("a.md", "text/markdown"));
        assertEquals(SourceFileKind.MARKDOWN, SourceFileKind.resolve("a.markdown", "text/plain"));
        assertEquals(SourceFileKind.TEXT, SourceFileKind.resolve("a.txt", "text/plain"));
        assertEquals(SourceFileKind.HTML, SourceFileKind.resolve("a.html", "text/html"));
        assertEquals(SourceFileKind.HTML, SourceFileKind.resolve("a.htm", "application/xhtml+xml"));
        assertNull(SourceFileKind.resolve("a.csv", "text/csv"));
        assertNull(SourceFileKind.resolve("a.pdf", "text/plain"));
    }

    @Test
    void allFileKindsRequireDoclingHybrid() {
        for (SourceFileKind kind : SourceFileKind.values()) {
            assertTrue(kind.requiresDocling(), kind.name());
        }
        assertEquals("pptx", SourceFileKind.PPTX.doclingFromFormat());
        assertEquals("ppt", SourceFileKind.PPT.doclingFromFormat());
        assertEquals("xlsx", SourceFileKind.XLSX.doclingFromFormat());
        assertEquals("xls", SourceFileKind.XLS.doclingFromFormat());
        assertTrue(SourceFileKind.PPTX.zipContainer());
        assertTrue(SourceFileKind.XLSX.zipContainer());
        assertTrue(SourceFileKind.PPT.oleContainer());
        assertTrue(SourceFileKind.XLS.oleContainer());
        assertFalse(SourceFileKind.TEXT.binary());
    }
}
