package com.g2rain.knowledge.client.model;

/** Position and relevance score returned by the configured rerank provider. */
public record RerankScore(int index, float score) {
}
