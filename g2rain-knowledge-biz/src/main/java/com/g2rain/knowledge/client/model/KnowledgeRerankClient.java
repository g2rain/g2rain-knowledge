package com.g2rain.knowledge.client.model;

import java.util.List;

public interface KnowledgeRerankClient {
    /**
     * Reranks verified evidence texts via Model Nexus {@code POST /v1/rerank}.
     *
     * @param query     retrieval query
     * @param documents verified candidate contents in original order
     * @param topN      max results requested from Nexus ({@code top_n})
     */
    List<RerankScore> rerank(String query, List<String> documents, int topN);
}
