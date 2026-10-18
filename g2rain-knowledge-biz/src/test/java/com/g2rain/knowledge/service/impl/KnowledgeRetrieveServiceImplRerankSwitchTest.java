package com.g2rain.knowledge.service.impl;

import com.g2rain.knowledge.client.milvus.MilvusKnowledgeClient;
import com.g2rain.knowledge.client.milvus.MilvusProperties;
import com.g2rain.knowledge.client.milvus.MilvusSearchCandidate;
import com.g2rain.knowledge.client.model.EmbeddingClient;
import com.g2rain.knowledge.client.model.KnowledgeRerankClient;
import com.g2rain.knowledge.client.model.KnowledgeRerankProperties;
import com.g2rain.knowledge.client.model.RerankScore;
import com.g2rain.knowledge.client.retrieve.KnowledgeRetrieveProperties;
import com.g2rain.knowledge.client.storage.KnowledgeStorageClient;
import com.g2rain.knowledge.dao.KnowledgeChunkDao;
import com.g2rain.knowledge.dao.KnowledgeItemDao;
import com.g2rain.knowledge.dao.KnowledgeReleaseDao;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.po.KnowledgeChunkPo;
import com.g2rain.knowledge.dao.po.KnowledgeItemPo;
import com.g2rain.knowledge.dao.po.KnowledgeReleasePo;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.domain.ContentHashes;
import com.g2rain.knowledge.dto.KnowledgeRetrieveDto;
import com.g2rain.knowledge.enums.KnowledgeEnablementStatus;
import com.g2rain.knowledge.enums.KnowledgeRevisionStatus;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.vo.KnowledgeRetrieveVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeRetrieveServiceImplRerankSwitchTest {

    @Mock private KnowledgeAccessService knowledgeAccessService;
    @Mock private KnowledgeChunkDao knowledgeChunkDao;
    @Mock private KnowledgeRevisionDao knowledgeRevisionDao;
    @Mock private KnowledgeReleaseDao knowledgeReleaseDao;
    @Mock private KnowledgeItemDao knowledgeItemDao;
    @Mock private KnowledgeStorageClient knowledgeStorageClient;
    @Mock private EmbeddingClient embeddingClient;
    @Mock private KnowledgeRerankClient knowledgeRerankClient;
    @Mock private MilvusKnowledgeClient milvusKnowledgeClient;
    @Mock private MilvusProperties milvusProperties;
    @Mock private KnowledgeRetrieveProperties retrieveProperties;
    @Mock private KnowledgeRerankProperties rerankProperties;
    @InjectMocks private KnowledgeRetrieveServiceImpl service;

    @BeforeEach
    void setUp() {
        when(retrieveProperties.getTotalTimeoutMs()).thenReturn(8000L);
        when(retrieveProperties.getMaxTopK()).thenReturn(20);
        when(knowledgeAccessService.requireOrganId()).thenReturn(1L);
        when(embeddingClient.embed(anyList())).thenReturn(List.of(new float[] {0.1f}));
        when(milvusProperties.getCollectionName()).thenReturn("chunks");
        when(milvusKnowledgeClient.search(anyString(), any(), any(), any(), any(), anyInt()))
            .thenReturn(List.of(new MilvusSearchCandidate(11L, 0.4f)));
        when(knowledgeChunkDao.selectById(11L)).thenReturn(chunk());
        when(knowledgeRevisionDao.selectById(21L)).thenReturn(revision());
        when(knowledgeReleaseDao.selectList(any())).thenReturn(List.of(release()));
        when(knowledgeItemDao.selectById(31L)).thenReturn(item());
        when(knowledgeStorageClient.readParsedChunk(anyString(), anyString())).thenReturn("退款三个工作日到账");
    }

    @Test
    void skipsRerankApiWhenDisabled() {
        when(rerankProperties.isEnabled()).thenReturn(false);

        KnowledgeRetrieveVo result = service.retrieve(request());

        assertEquals(11L, result.getSources().getFirst().getChunkId());
        verify(knowledgeRerankClient, never()).rerank(anyString(), anyList(), anyInt());
    }

    @Test
    void callsRerankApiWhenEnabled() {
        when(rerankProperties.isEnabled()).thenReturn(true);
        when(knowledgeRerankClient.rerank(anyString(), anyList(), anyInt()))
            .thenReturn(List.of(new RerankScore(0, 0.9f)));

        KnowledgeRetrieveVo result = service.retrieve(request());

        assertEquals(0.9f, result.getSources().getFirst().getScore());
        verify(knowledgeRerankClient).rerank(anyString(), anyList(), anyInt());
    }

    private static KnowledgeRetrieveDto request() {
        KnowledgeRetrieveDto dto = new KnowledgeRetrieveDto();
        dto.setQuery("退款多久到账");
        dto.setTopK(5);
        return dto;
    }

    private static KnowledgeChunkPo chunk() {
        KnowledgeChunkPo chunk = new KnowledgeChunkPo();
        chunk.setId(11L);
        chunk.setOrganId(1L);
        chunk.setRevisionId(21L);
        chunk.setChunkStatus("INDEXED");
        chunk.setSourceLocator("char:0:8");
        chunk.setContentSha256(ContentHashes.sha256Utf8("退款三个工作日到账"));
        return chunk;
    }

    private static KnowledgeRevisionPo revision() {
        KnowledgeRevisionPo revision = new KnowledgeRevisionPo();
        revision.setId(21L);
        revision.setOrganId(1L);
        revision.setKnowledgeId(31L);
        revision.setRevisionStatus(KnowledgeRevisionStatus.PUBLISHED.name());
        revision.setIndexStatus("INDEXED");
        revision.setParsedStoragePath("parsed.md");
        revision.setRevisionNo("v1");
        return revision;
    }

    private static KnowledgeReleasePo release() {
        KnowledgeReleasePo release = new KnowledgeReleasePo();
        release.setActiveRevisionId(21L);
        return release;
    }

    private static KnowledgeItemPo item() {
        KnowledgeItemPo item = new KnowledgeItemPo();
        item.setId(31L);
        item.setOrganId(1L);
        item.setStatus(KnowledgeEnablementStatus.ACTIVE.name());
        item.setKnowledgeName("退款制度");
        return item;
    }
}
