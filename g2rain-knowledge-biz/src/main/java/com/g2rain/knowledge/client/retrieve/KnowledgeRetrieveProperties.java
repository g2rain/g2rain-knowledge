package com.g2rain.knowledge.client.retrieve;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "g2rain.knowledge.retrieve")
public class KnowledgeRetrieveProperties {
    private int defaultTopK = 5;
    private int maxTopK = 20;
    /** End-to-end retrieve budget in milliseconds ({@code KNOWLEDGE_RETRIEVE_TOTAL_TIMEOUT_MS}). */
    private long totalTimeoutMs = 8000L;
}
