package com.g2rain.knowledge.client.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Settings for the internal Model Nexus OpenAI-compatible embedding endpoint. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "g2rain.knowledge.embedding")
public class EmbeddingProperties {
    /** Full endpoint, for example {@code http://g2rain-model-nexus:8080/v1/embeddings}. */
    private String url = "";
    private String modelName = "";
    private int dimension = 4096;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(30);
}
