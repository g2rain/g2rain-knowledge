package com.g2rain.knowledge.client.docling;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.domain.ChunkContent;
import com.g2rain.knowledge.domain.ChunkingResult;
import com.g2rain.knowledge.domain.KnowledgeChunkPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoclingServeClientTest {
    private final DoclingServeClient client =
        new DoclingServeClient(new DoclingProperties(), new KnowledgeChunkPolicy());

    @Test
    void normalizesSuccessfulConvertResponse() {
        ParsedDocument result = DoclingServeClient.normalizeConvert(
            new DoclingServeClient.DoclingConvertResponse("success",
                new DoclingServeClient.DoclingDocument("# title", "title")));

        assertEquals("# title", result.markdown());
        assertEquals("title", result.text());
    }

    @Test
    void rejectsFailureOrEmptyConvertContent() {
        assertThrows(BusinessException.class, () -> DoclingServeClient.normalizeConvert(
            new DoclingServeClient.DoclingConvertResponse("failure", null)));
        assertThrows(BusinessException.class, () -> DoclingServeClient.normalizeConvert(
            new DoclingServeClient.DoclingConvertResponse("success",
                new DoclingServeClient.DoclingDocument("", ""))));
    }

    @Test
    void normalizesHybridChunkResponse() {
        DoclingServeClient.ChunkDocumentResponse response = new DoclingServeClient.ChunkDocumentResponse(
            List.of(new DoclingServeClient.ChunkedDocumentResultItem(
                "a.pdf", 0, "Head > body contextual", "body raw", 12,
                List.of("Head", "Sub"), null, List.of("#/texts/0"), List.of(2, 3))),
            List.of(new DoclingServeClient.ExportResult(
                new DoclingServeClient.ExportDocumentContent("# Head\n\nbody raw", "body raw"),
                "success")),
            1.2
        );

        ChunkingResult result = client.normalizeChunk(response);
        assertEquals("# Head\n\nbody raw", result.documentMarkdown());
        assertEquals(KnowledgeChunkPolicy.PARSER_DOCLING_HYBRID, result.parserName());
        assertEquals(1, result.chunks().size());
        ChunkContent chunk = result.chunks().getFirst();
        assertEquals(1, chunk.chunkNo());
        assertEquals("chunk:1", chunk.sourceLocator());
        assertEquals("body raw", chunk.rawText());
        assertEquals("Head > body contextual", chunk.embeddingText());
        assertEquals(12, chunk.tokenCount());
        assertEquals(2, chunk.pageStart());
        assertEquals(3, chunk.pageEnd());
        assertEquals("[\"Head\",\"Sub\"]", chunk.titlePath());
        assertTrue(chunk.contentSha256().length() == 64);
    }

    @Test
    void rejectsEmptyHybridChunks() {
        assertThrows(BusinessException.class, () -> client.normalizeChunk(
            new DoclingServeClient.ChunkDocumentResponse(List.of(), List.of(), 0.1)));
    }
}
