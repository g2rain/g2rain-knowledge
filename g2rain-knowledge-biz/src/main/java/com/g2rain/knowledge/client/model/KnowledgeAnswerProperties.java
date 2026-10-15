package com.g2rain.knowledge.client.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "g2rain.knowledge.answer")
public class KnowledgeAnswerProperties {
    /** Full endpoint, for example {@code http://g2rain-model-nexus:8080/v1/chat/completions}. */
    private String url = "";
    private String modelName = "";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(60);
}
