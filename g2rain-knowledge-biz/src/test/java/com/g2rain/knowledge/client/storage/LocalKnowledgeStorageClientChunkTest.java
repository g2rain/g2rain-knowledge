package com.g2rain.knowledge.client.storage;

import com.g2rain.knowledge.domain.ChunkContent;
import com.g2rain.knowledge.domain.ContentHashes;
import com.g2rain.knowledge.domain.KnowledgeChunkPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalKnowledgeStorageClientChunkTest {
    @TempDir
    Path tempDir;

    @Test
    void writesChunksJsonAndReadsRawVersusEmbedding() {
        KnowledgeStorageProperties properties = new KnowledgeStorageProperties();
        properties.setRoot(tempDir.toString());
        LocalKnowledgeStorageClient client = new LocalKnowledgeStorageClient(properties);
        KnowledgeChunkPolicy policy = new KnowledgeChunkPolicy();
        ChunkContent chunk = policy.build(1, "chunk:1", "raw body", "Title > raw body", 4, 1, 1, "Title");

        String path = client.writeParsedArtifacts(1L, 2L, 3L, 9L, "# Title\n\nraw body", List.of(chunk));
        assertEquals("raw body", client.readParsedChunk(path, "chunk:1"));
        assertEquals("Title > raw body", client.readChunkForEmbedding(path, "chunk:1"));
        assertEquals(ContentHashes.sha256Utf8("raw body"), chunk.contentSha256());
    }
}
