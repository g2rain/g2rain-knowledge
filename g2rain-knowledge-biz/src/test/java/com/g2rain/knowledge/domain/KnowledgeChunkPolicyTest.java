package com.g2rain.knowledge.domain;

import com.g2rain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeChunkPolicyTest {
    private final KnowledgeChunkPolicy policy = new KnowledgeChunkPolicy();
    private final ChunkingProfile profile = ChunkingProfile.defaults(32);

    @Test
    void chunkFaqKeepsQuestionInEmbeddingWhenSplit() {
        String question = "退款多久到账？";
        String answer = "一".repeat(80);
        List<ChunkContent> chunks = policy.chunkFaq(question, answer, profile);
        assertTrue(chunks.size() > 1);
        for (ChunkContent chunk : chunks) {
            assertTrue(chunk.embeddingText().contains(question));
            assertTrue(chunk.sourceLocator().startsWith("chunk:"));
        }
        policy.validateManifest(chunks);
    }

    @Test
    void chunkFaqSingleChunkIncludesQuestionInRaw() {
        List<ChunkContent> chunks = policy.chunkFaq("Q1", "短答", profile);
        assertEquals(1, chunks.size());
        assertTrue(chunks.getFirst().rawText().contains("Q1"));
        assertTrue(chunks.getFirst().rawText().contains("短答"));
        assertEquals("[\"Q1\"]", chunks.getFirst().titlePath());
        policy.validateManifest(chunks);
    }

    @Test
    void validateManifestRejectsBrokenSequence() {
        ChunkContent ok = policy.build(1, "chunk:1", "a", "a", 1, 1, 1, "[\"T\"]");
        ChunkContent badNo = policy.build(3, "chunk:3", "b", "b", 1, null, null, null);
        assertThrows(BusinessException.class, () -> policy.validateManifest(List.of(ok, badNo)));
    }

    @Test
    void estimateTokensCountsCjk() {
        assertTrue(policy.estimateTokens("退款三个工作日") >= 7);
        assertTrue(policy.estimateTokens("hello world") >= 2);
    }
}
