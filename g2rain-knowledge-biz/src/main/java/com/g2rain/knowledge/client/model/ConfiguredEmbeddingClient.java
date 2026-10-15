package com.g2rain.knowledge.client.model;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Calls the configured embedding URL. Only server-configured model
 * and dimension are used; vendor credentials stay inside Nexus.
 */
@Component
public class ConfiguredEmbeddingClient implements EmbeddingClient {
    private final EmbeddingProperties properties;

    public ConfiguredEmbeddingClient(EmbeddingProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (isBlank(properties.getUrl()) || isBlank(properties.getModelName()) || texts == null || texts.isEmpty()) {
            throw new BusinessException(KnowledgeErrorCode.EMBEDDING_NOT_CONFIGURED);
        }
        try {
            EmbeddingResponse response = ModelNexusRestClients
                .create(properties.getConnectTimeout(), properties.getReadTimeout())
                .post()
                .uri(properties.getUrl())
                .body(new EmbeddingRequest(properties.getModelName(), texts, properties.getDimension()))
                .retrieve()
                .body(EmbeddingResponse.class);
            return normalize(response, texts.size(), properties.getDimension());
        } catch (RestClientException exception) {
            throw new BusinessException(KnowledgeErrorCode.EMBEDDING_OPERATION_FAILED);
        }
    }

    static List<float[]> normalize(EmbeddingResponse response, int expectedCount, int dimension) {
        if (response == null || response.data() == null || response.data().size() != expectedCount) {
            throw new BusinessException(KnowledgeErrorCode.EMBEDDING_INVALID_RESPONSE);
        }
        List<EmbeddingData> ordered = new ArrayList<>(response.data());
        ordered.sort(Comparator.comparing(EmbeddingData::index, Comparator.nullsLast(Integer::compareTo)));
        List<float[]> vectors = new ArrayList<>(expectedCount);
        for (int index = 0; index < ordered.size(); index++) {
            EmbeddingData data = ordered.get(index);
            if (data.index() == null || data.index() != index || data.embedding() == null || data.embedding().length != dimension) {
                throw new BusinessException(KnowledgeErrorCode.EMBEDDING_INVALID_RESPONSE);
            }
            for (float value : data.embedding()) {
                if (!Float.isFinite(value)) {
                    throw new BusinessException(KnowledgeErrorCode.EMBEDDING_INVALID_RESPONSE);
                }
            }
            vectors.add(data.embedding());
        }
        return List.copyOf(vectors);
    }

    private static boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }

    record EmbeddingRequest(String model, List<String> input, Integer dimensions) {
    }

    record EmbeddingResponse(List<EmbeddingData> data) {
    }

    record EmbeddingData(Integer index, float[] embedding) {
    }
}
