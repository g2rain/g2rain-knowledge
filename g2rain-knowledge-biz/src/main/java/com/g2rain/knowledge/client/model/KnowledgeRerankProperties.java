package com.g2rain.knowledge.client.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "g2rain.knowledge.rerank")
public class KnowledgeRerankProperties {
    /** When false, retrieve keeps MySQL-verified order and does not call the rerank API. */
    private boolean enabled = true;
    /** Full endpoint, for example {@code http://g2rain-model-nexus:8080/v1/rerank}. */
    private String url = "";
    private String modelName = "";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(30);
}
