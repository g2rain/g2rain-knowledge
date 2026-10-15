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
    void storesOnlyUnderGeneratedTenantRevisionPath() throws Exception {
        LocalKnowledgeStorageClient client = clientFor(temporaryDirectory);
        byte[] content = "%PDF-1.7\nsource".getBytes(StandardCharsets.US_ASCII);

        StoredSource stored = client.storeSource(1L, 2L, 3L, new ByteArrayInputStream(content),
            new SourceMetadata("../refund-policy.pdf", "application/pdf", content.length));

        assertTrue(stored.relativePath().startsWith("1/2/3/source/"));
        assertTrue(stored.relativePath().endsWith(".pdf"));
        assertEquals("refund-policy.pdf", stored.displayFileName());
        assertEquals(content.length, stored.size());
        assertTrue(Files.isRegularFile(temporaryDirectory.resolve(stored.relativePath())));

        client.deleteUnreferenced(stored.relativePath());
        assertFalse(Files.exists(temporaryDirectory.resolve(stored.relativePath())));
    }

    @Test
    void rejectsWrongMimeOrMissingPdfSignature() {
        LocalKnowledgeStorageClient client = clientFor(temporaryDirectory);
        assertThrows(BusinessException.class, () -> client.storeSource(1L, 2L, 3L,
            new ByteArrayInputStream("not a pdf".getBytes(StandardCharsets.US_ASCII)),
            new SourceMetadata("manual.pdf", "application/pdf", 9)));
        assertThrows(BusinessException.class, () -> client.storeSource(1L, 2L, 3L,
            new ByteArrayInputStream("%PDF-1.7".getBytes(StandardCharsets.US_ASCII)),
            new SourceMetadata("manual.pdf", "text/plain", 8)));
    }

    private LocalKnowledgeStorageClient clientFor(Path root) {
        KnowledgeStorageProperties properties = new KnowledgeStorageProperties();
        properties.setRoot(root.toString());
        return new LocalKnowledgeStorageClient(properties);
    }
}
