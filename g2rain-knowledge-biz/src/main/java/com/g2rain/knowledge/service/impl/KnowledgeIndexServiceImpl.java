package com.g2rain.knowledge.service.impl;

import com.g2rain.common.json.JsonCodec;
import com.g2rain.common.json.JsonCodecFactory;
import com.g2rain.knowledge.client.milvus.MilvusIndexEntity;
import com.g2rain.knowledge.client.milvus.MilvusKnowledgeClient;
import com.g2rain.knowledge.client.milvus.MilvusProperties;
import com.g2rain.knowledge.client.model.EmbeddingClient;
import com.g2rain.knowledge.client.model.EmbeddingProperties;
import com.g2rain.knowledge.client.storage.KnowledgeStorageClient;
import com.g2rain.knowledge.dao.KnowledgeChunkDao;
import com.g2rain.knowledge.dao.KnowledgeItemDao;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.po.KnowledgeChunkPo;
import com.g2rain.knowledge.dao.po.KnowledgeItemPo;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.dto.KnowledgeChunkSelectDto;
import com.g2rain.knowledge.service.KnowledgeIndexService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeIndexServiceImpl implements KnowledgeIndexService {
    private static final JsonCodec JSON = JsonCodecFactory.instance();

    @Resource private KnowledgeRevisionDao knowledgeRevisionDao;
    @Resource private KnowledgeItemDao knowledgeItemDao;
    @Resource private KnowledgeChunkDao knowledgeChunkDao;
    @Resource private KnowledgeStorageClient knowledgeStorageClient;
    @Resource private EmbeddingClient embeddingClient;
    @Resource private EmbeddingProperties embeddingProperties;
    @Resource private MilvusKnowledgeClient milvusKnowledgeClient;
    @Resource private MilvusProperties milvusProperties;

    @Override
    public void index(Long revisionId) {
        KnowledgeRevisionPo revision = knowledgeRevisionDao.selectById(revisionId);
        KnowledgeItemPo item = knowledgeItemDao.selectById(revision.getKnowledgeId());
        KnowledgeChunkSelectDto query = new KnowledgeChunkSelectDto();
        query.setRevisionId(revisionId);
        List<KnowledgeChunkPo> chunks = knowledgeChunkDao.selectList(query);
        List<String> rawContents = chunks.stream()
            .map(c -> knowledgeStorageClient.readParsedChunk(revision.getParsedStoragePath(), c.getSourceLocator()))
            .toList();
        List<String> embeddingContents = chunks.stream()
            .map(c -> knowledgeStorageClient.readChunkForEmbedding(revision.getParsedStoragePath(), c.getSourceLocator()))
            .toList();
        List<float[]> vectors = embeddingClient.embed(embeddingContents);
        milvusKnowledgeClient.validateSchema(milvusProperties.getCollectionName(), vectors.getFirst().length);
        List<String> tags = tags(item == null ? null : item.getTagsJson());
        String embeddingProfile = embeddingProperties.getModelName() + "-" + embeddingProperties.getDimension();
        List<MilvusIndexEntity> entities = java.util.stream.IntStream.range(0, chunks.size()).mapToObj(i -> {
            KnowledgeChunkPo c = chunks.get(i);
            String visibility = item == null || item.getVisibility() == null || item.getVisibility().isBlank()
                ? "PRIVATE" : item.getVisibility();
            return new MilvusIndexEntity(c.getId(), revision.getOrganId(), revision.getKnowledgeId(), revisionId,
                tags, revision.getSourceType(), rawContents.get(i), c.getTitlePath(), c.getPageStart(), c.getPageEnd(),
                embeddingProfile, visibility, vectors.get(i));
        }).toList();
        milvusKnowledgeClient.upsertStaged(milvusProperties.getCollectionName(), entities);
        long indexedCount = milvusKnowledgeClient.countRevision(milvusProperties.getCollectionName(), revision.getOrganId(), revisionId);
        if (indexedCount != entities.size()) {
            throw new IllegalStateException("Milvus revision entity count mismatch");
        }
        milvusKnowledgeClient.verifyStaged(milvusProperties.getCollectionName(), entities);
    }

    private List<String> tags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }
        try {
            List<String> tags = JSON.str2obj(tagsJson, List.class, String.class);
            return tags == null ? List.of() : List.copyOf(tags);
        } catch (Exception exception) {
            throw new IllegalArgumentException("invalid item tags", exception);
        }
    }
}
