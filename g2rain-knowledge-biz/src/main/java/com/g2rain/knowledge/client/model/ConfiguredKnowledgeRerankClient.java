package com.g2rain.knowledge.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * OpenAI-compatible rerank adapter. The request URL comes from configuration.
 * Only verified evidence contents are passed.
 */
@Component
public class ConfiguredKnowledgeRerankClient implements KnowledgeRerankClient {
    private final KnowledgeRerankProperties properties;

    public ConfiguredKnowledgeRerankClient(KnowledgeRerankProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<RerankScore> rerank(String query, List<String> documents, int topN) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        if (blank(properties.getUrl()) || blank(properties.getModelName())) {
            throw new BusinessException(KnowledgeErrorCode.RERANK_NOT_CONFIGURED);
        }
        int clampedTopN = Math.max(1, Math.min(topN, documents.size()));
        try {
            RerankResponse response = ModelNexusRestClients
                .create(properties.getConnectTimeout(), properties.getReadTimeout())
                .post()
                .uri(properties.getUrl())
                .body(new RerankRequest(properties.getModelName(), query, documents, clampedTopN, false))
                .retrieve()
                .body(RerankResponse.class);
            return normalize(response, documents.size());
        } catch (RestClientException exception) {
            throw new BusinessException(KnowledgeErrorCode.RERANK_OPERATION_FAILED);
        }
    }

    static List<RerankScore> normalize(RerankResponse response, int documentCount) {
        if (response == null || response.results() == null || response.results().isEmpty()
            || response.results().size() > documentCount) {
            throw new BusinessException(KnowledgeErrorCode.RERANK_INVALID_RESPONSE);
        }
        Set<Integer> seen = new HashSet<>();
        List<RerankScore> scores = response.results().stream().map(result -> {
            if (result.index() == null || result.relevanceScore() == null
                || result.index() < 0 || result.index() >= documentCount
                || !Double.isFinite(result.relevanceScore()) || !seen.add(result.index())) {
                throw new BusinessException(KnowledgeErrorCode.RERANK_INVALID_RESPONSE);
            }
            return new RerankScore(result.index(), result.relevanceScore().floatValue());
        }).toList();
        return scores.stream().sorted(Comparator.comparing(RerankScore::score).reversed()).toList();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    record RerankRequest(
        String model,
        String query,
        List<String> documents,
        @JsonProperty("top_n") Integer topN,
        @JsonProperty("return_documents") Boolean returnDocuments
    ) {
    }

    record RerankResponse(List<RerankResult> results) {
    }

    record RerankResult(Integer index, @JsonProperty("relevance_score") Double relevanceScore) {
    }
}
