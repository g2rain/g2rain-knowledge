package com.g2rain.knowledge.service.impl;

import com.g2rain.knowledge.client.milvus.MilvusKnowledgeClient;
import com.g2rain.knowledge.client.milvus.MilvusProperties;
import com.g2rain.knowledge.client.milvus.MilvusSearchCandidate;
import com.g2rain.knowledge.client.model.EmbeddingClient;
import com.g2rain.knowledge.client.model.KnowledgeRerankClient;
import com.g2rain.knowledge.client.model.KnowledgeRerankProperties;
import com.g2rain.knowledge.client.model.RerankScore;
import com.g2rain.knowledge.client.retrieve.KnowledgeRetrieveProperties;
import com.g2rain.knowledge.dto.KnowledgeRetrieveDto;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgePublicEvidenceVerifier;
import com.g2rain.knowledge.vo.KnowledgeEvidenceVo;
import com.g2rain.knowledge.vo.KnowledgeRetrieveVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeRetrieveServiceImplRerankSwitchTest {

    @Mock private KnowledgeAccessService knowledgeAccessService;
    @Mock private KnowledgePublicEvidenceVerifier knowledgePublicEvidenceVerifier;
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
        when(milvusKnowledgeClient.search(anyString(), anyLong(), anyBoolean(), any(), any(), any(), anyInt()))
            .thenReturn(List.of(new MilvusSearchCandidate(11L, 0.4f)));
        when(knowledgePublicEvidenceVerifier.verifyCandidate(anyLong(), anyBoolean(), any()))
            .thenReturn(Optional.of(evidence()));
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

    private static KnowledgeEvidenceVo evidence() {
        KnowledgeEvidenceVo vo = new KnowledgeEvidenceVo();
        vo.setChunkId(11L);
        vo.setKnowledgeId(31L);
        vo.setRevisionId(21L);
        vo.setContent("退款三个工作日到账");
        vo.setScore(0.4f);
        return vo;
    }
}
