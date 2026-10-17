package com.g2rain.knowledge.client.storage;

import com.g2rain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalKnowledgeStorageClientTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void storesPdfUnderGeneratedTenantRevisionPath() throws Exception {
        LocalKnowledgeStorageClient client = clientFor(temporaryDirectory);
        byte[] content = "%PDF-1.7\nsource".getBytes(StandardCharsets.US_ASCII);

        StoredSource stored = client.storeSource(1L, 2L, 3L, new ByteArrayInputStream(content),
            new SourceMetadata("../refund-policy.pdf", "application/pdf", content.length));

        assertTrue(stored.relativePath().startsWith("1/2/3/source/"));
        assertTrue(stored.relativePath().endsWith(".pdf"));
        assertEquals("refund-policy.pdf", stored.displayFileName());
        assertEquals("application/pdf", stored.contentType());
        assertEquals(content.length, stored.size());
        assertTrue(Files.isRegularFile(temporaryDirectory.resolve(stored.relativePath())));

        client.deleteUnreferenced(stored.relativePath());
        assertFalse(Files.exists(temporaryDirectory.resolve(stored.relativePath())));
    }

    @Test
    void storesMarkdownAndHtmlAsUtf8Text() throws Exception {
        LocalKnowledgeStorageClient client = clientFor(temporaryDirectory);
        byte[] markdown = "# 退款\n三个工作日到账".getBytes(StandardCharsets.UTF_8);
        byte[] html = "<h1>退款</h1><p>三个工作日到账</p>".getBytes(StandardCharsets.UTF_8);

        StoredSource md = client.storeSource(1L, 2L, 3L, new ByteArrayInputStream(markdown),
            new SourceMetadata("refund.md", "text/markdown", markdown.length));
        StoredSource htmlStored = client.storeSource(1L, 2L, 4L, new ByteArrayInputStream(html),
            new SourceMetadata("refund.html", "text/html", html.length));
        StoredSource plainMd = client.storeSource(1L, 2L, 5L, new ByteArrayInputStream(markdown),
            new SourceMetadata("refund.md", "text/plain", markdown.length));

        assertTrue(md.relativePath().endsWith(".md"));
        assertEquals("text/markdown", md.contentType());
        assertTrue(htmlStored.relativePath().endsWith(".html"));
        assertEquals("text/html", htmlStored.contentType());
        assertEquals("text/markdown", plainMd.contentType());
        assertEquals("# 退款\n三个工作日到账",
            Files.readString(temporaryDirectory.resolve(md.relativePath()), StandardCharsets.UTF_8));
    }

    @Test
    void rejectsWrongMimeMissingPdfSignatureAndUnsupportedExtension() {
        LocalKnowledgeStorageClient client = clientFor(temporaryDirectory);
        assertThrows(BusinessException.class, () -> client.storeSource(1L, 2L, 3L,
            new ByteArrayInputStream("not a pdf".getBytes(StandardCharsets.US_ASCII)),
            new SourceMetadata("manual.pdf", "application/pdf", 9)));
        assertThrows(BusinessException.class, () -> client.storeSource(1L, 2L, 3L,
            new ByteArrayInputStream("%PDF-1.7".getBytes(StandardCharsets.US_ASCII)),
            new SourceMetadata("manual.pdf", "text/plain", 8)));
        assertThrows(BusinessException.class, () -> client.storeSource(1L, 2L, 3L,
            new ByteArrayInputStream("# hello".getBytes(StandardCharsets.UTF_8)),
            new SourceMetadata("notes.txt", "text/plain", 7)));
        assertThrows(BusinessException.class, () -> client.storeSource(1L, 2L, 3L,
            new ByteArrayInputStream(new byte[] {(byte) 0xff, (byte) 0xfe, 0x00}),
            new SourceMetadata("broken.md", "text/markdown", 3)));
    }

    private LocalKnowledgeStorageClient clientFor(Path root) {
        KnowledgeStorageProperties properties = new KnowledgeStorageProperties();
        properties.setRoot(root.toString());
        return new LocalKnowledgeStorageClient(properties);
    }
}
