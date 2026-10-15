package com.g2rain.knowledge.client.model;

import com.g2rain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfiguredEmbeddingClientTest {
    @Test
    void ordersAndValidatesVectorsByResponseIndex() {
        List<float[]> vectors = ConfiguredEmbeddingClient.normalize(new ConfiguredEmbeddingClient.EmbeddingResponse(List.of(
            new ConfiguredEmbeddingClient.EmbeddingData(1, new float[]{3, 4}),
            new ConfiguredEmbeddingClient.EmbeddingData(0, new float[]{1, 2})
        )), 2, 2);

        assertEquals(2, vectors.size());
        assertArrayEquals(new float[]{1, 2}, vectors.getFirst());
    }

    @Test
    void rejectsWrongDimensionAndNonFiniteValues() {
        assertThrows(BusinessException.class, () -> ConfiguredEmbeddingClient.normalize(
            new ConfiguredEmbeddingClient.EmbeddingResponse(List.of(new ConfiguredEmbeddingClient.EmbeddingData(0, new float[]{1}))), 1, 2));
        assertThrows(BusinessException.class, () -> ConfiguredEmbeddingClient.normalize(
            new ConfiguredEmbeddingClient.EmbeddingResponse(List.of(new ConfiguredEmbeddingClient.EmbeddingData(0, new float[]{Float.NaN}))), 1, 1));
    }
}
