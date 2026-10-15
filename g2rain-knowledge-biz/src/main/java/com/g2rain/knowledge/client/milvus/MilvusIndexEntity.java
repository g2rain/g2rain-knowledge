package com.g2rain.knowledge.client.milvus;

import java.util.List;

/** The complete staged projection for one knowledge chunk. */
public record MilvusIndexEntity(Long chunkId, Long organId, Long knowledgeId, Long revisionId,
                                List<String> tags, String sourceType, String content, String titlePath,
                                Integer pageStart, Integer pageEnd, String embeddingProfile, float[] vector) {
}
