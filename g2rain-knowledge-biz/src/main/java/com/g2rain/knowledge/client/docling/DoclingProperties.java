package com.g2rain.knowledge.client.docling;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * The API key is injected only from deployment configuration and is never
 * exposed through a management DTO or logged.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "g2rain.knowledge.docling")
public class DoclingProperties {
    private String baseUrl = "";
    private String apiKey = "";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(90);
    /** HybridChunker max_tokens; mapped to multipart chunking_max_tokens. */
    private int chunkingMaxTokens = 512;
}
