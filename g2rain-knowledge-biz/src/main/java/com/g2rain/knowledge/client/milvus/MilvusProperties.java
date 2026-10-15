package com.g2rain.knowledge.client.milvus;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "g2rain.knowledge.milvus")
public class MilvusProperties {
    private String uri = "";
    private String token = "";
    private String collectionName = "g2rain_knowledge_chunks_qwen_4096_v1";
}
