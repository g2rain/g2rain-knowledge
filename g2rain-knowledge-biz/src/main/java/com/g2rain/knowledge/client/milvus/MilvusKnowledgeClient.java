package com.g2rain.knowledge.client.milvus;

import java.util.List;

/**
 * Vector projection boundary. All writes are staged with searchable=false;
 * publishing is the only operation allowed to expose a revision to retrieval.
 */
public interface MilvusKnowledgeClient {
    void validateSchema(String collectionName, int dimension);
    void upsertStaged(String collectionName, List<MilvusIndexEntity> entities);

    long countRevision(String collectionName, Long organId, Long revisionId);

    void verifyStaged(String collectionName, List<MilvusIndexEntity> expected);

    void setSearchable(String collectionName, Long organId, Long revisionId, boolean searchable);

    List<MilvusSearchCandidate> search(String collectionName, Long organId, List<String> allTagCodes,
                                        List<String> anyTagCodes, float[] vector, int topK);
}
