package com.g2rain.knowledge.client.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * Server-owned file storage settings. The root is deliberately never accepted
 * from an HTTP request.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "g2rain.knowledge.storage")
public class KnowledgeStorageProperties {
    private String root = "";
    private DataSize maxFileSize = DataSize.ofMegabytes(10);
}
