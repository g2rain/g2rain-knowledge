package com.g2rain.knowledge.client.model;

import com.g2rain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfiguredKnowledgeRerankClientTest {
    @Test
    void acceptsFullResultSetAndOrdersByScoreDescending() {
        List<RerankScore> scores = ConfiguredKnowledgeRerankClient.normalize(
            new ConfiguredKnowledgeRerankClient.RerankResponse(List.of(
                new ConfiguredKnowledgeRerankClient.RerankResult(0, 0.2),
                new ConfiguredKnowledgeRerankClient.RerankResult(1, 0.9),
                new ConfiguredKnowledgeRerankClient.RerankResult(2, 0.5)
            )),
            3);

        assertEquals(3, scores.size());
        assertEquals(1, scores.get(0).index());
        assertEquals(2, scores.get(1).index());
        assertEquals(0, scores.get(2).index());
    }

    @Test
    void acceptsTopNSubsetSmallerThanDocumentCount() {
        List<RerankScore> scores = ConfiguredKnowledgeRerankClient.normalize(
            new ConfiguredKnowledgeRerankClient.RerankResponse(List.of(
                new ConfiguredKnowledgeRerankClient.RerankResult(2, 0.95),
                new ConfiguredKnowledgeRerankClient.RerankResult(0, 0.8)
            )),
            5);

        assertEquals(2, scores.size());
        assertEquals(2, scores.getFirst().index());
        assertEquals(0.95f, scores.getFirst().score());
    }

    @Test
    void rejectsDuplicateIndexNonFiniteScoreAndOversizedResults() {
        assertThrows(BusinessException.class, () -> ConfiguredKnowledgeRerankClient.normalize(
            new ConfiguredKnowledgeRerankClient.RerankResponse(List.of(
                new ConfiguredKnowledgeRerankClient.RerankResult(0, 0.1),
                new ConfiguredKnowledgeRerankClient.RerankResult(0, 0.2)
            )),
            2));
        assertThrows(BusinessException.class, () -> ConfiguredKnowledgeRerankClient.normalize(
            new ConfiguredKnowledgeRerankClient.RerankResponse(List.of(
                new ConfiguredKnowledgeRerankClient.RerankResult(0, Double.NaN)
            )),
            1));
        assertThrows(BusinessException.class, () -> ConfiguredKnowledgeRerankClient.normalize(
            new ConfiguredKnowledgeRerankClient.RerankResponse(List.of(
                new ConfiguredKnowledgeRerankClient.RerankResult(0, 0.1),
                new ConfiguredKnowledgeRerankClient.RerankResult(1, 0.2),
                new ConfiguredKnowledgeRerankClient.RerankResult(2, 0.3)
            )),
            2));
    }
}
