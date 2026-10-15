package com.g2rain.knowledge.service.impl;

import com.g2rain.knowledge.client.milvus.MilvusIndexEntity;
import com.g2rain.knowledge.client.milvus.MilvusKnowledgeClient;
import com.g2rain.knowledge.client.milvus.MilvusProperties;
import com.g2rain.knowledge.client.model.EmbeddingClient;
import com.g2rain.knowledge.client.storage.KnowledgeStorageClient;
import com.g2rain.knowledge.dao.KnowledgeChunkDao;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.po.KnowledgeChunkPo;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.dto.KnowledgeChunkSelectDto;
import com.g2rain.knowledge.service.KnowledgeIndexService;
import jakarta.annotation.Resource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class KnowledgeIndexServiceImpl implements KnowledgeIndexService {
    @Resource private KnowledgeRevisionDao knowledgeRevisionDao;
    @Resource private KnowledgeChunkDao knowledgeChunkDao;
    @Resource private KnowledgeStorageClient knowledgeStorageClient;
    @Resource private EmbeddingClient embeddingClient;
    @Resource private MilvusKnowledgeClient milvusKnowledgeClient;
    @Resource private MilvusProperties milvusProperties;
    @Resource private ObjectMapper objectMapper;

    @Override public void index(Long revisionId) {
        KnowledgeRevisionPo revision = knowledgeRevisionDao.selectById(revisionId);
        KnowledgeChunkSelectDto query = new KnowledgeChunkSelectDto(); query.setRevisionId(revisionId);
        List<KnowledgeChunkPo> chunks = knowledgeChunkDao.selectList(query);
        List<String> contents = chunks.stream().map(c -> knowledgeStorageClient.readParsedChunk(revision.getParsedStoragePath(), c.getSourceLocator())).toList();
        List<float[]> vectors = embeddingClient.embed(contents);
        milvusKnowledgeClient.validateSchema(milvusProperties.getCollectionName(), vectors.getFirst().length);
        List<String> tags = tags(revision.getTagsJson());
        List<MilvusIndexEntity> entities = java.util.stream.IntStream.range(0, chunks.size()).mapToObj(i -> {
            KnowledgeChunkPo c = chunks.get(i);
            return new MilvusIndexEntity(c.getId(), revision.getOrganId(), revision.getKnowledgeId(), revisionId,
                tags, revision.getSourceType(), contents.get(i), c.getTitlePath(), c.getPageStart(), c.getPageEnd(),
                revision.getEmbeddingProfile(), vectors.get(i));
        }).toList();
        milvusKnowledgeClient.upsertStaged(milvusProperties.getCollectionName(), entities);
        long indexedCount = milvusKnowledgeClient.countRevision(milvusProperties.getCollectionName(), revision.getOrganId(), revisionId);
        if (indexedCount != entities.size()) throw new IllegalStateException("Milvus revision entity count mismatch");
        milvusKnowledgeClient.verifyStaged(milvusProperties.getCollectionName(), entities);
    }

    private List<String> tags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) return List.of();
        try { return List.copyOf(objectMapper.readValue(tagsJson, new TypeReference<List<String>>() { })); }
        catch (Exception exception) { throw new IllegalArgumentException("invalid revision tags", exception); }
    }
}
