package com.g2rain.knowledge.client.model;

import java.util.List;

/** A provider-neutral boundary for batch text vectorization. */
public interface EmbeddingClient {
    List<float[]> embed(List<String> texts);
}
