package com.g2rain.knowledge.service.impl;

import com.g2rain.common.id.IdGenerator;
import com.g2rain.knowledge.dao.KnowledgeItemDao;
import com.g2rain.knowledge.dao.po.KnowledgeItemPo;
import com.g2rain.knowledge.domain.KnowledgeTagPolicy;
import com.g2rain.knowledge.dto.KnowledgeCreateDto;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeSaveSupport;
import com.g2rain.knowledge.service.KnowledgeTagGovernanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeDraftServiceImplTest {

    @Mock
    private KnowledgeItemDao knowledgeItemDao;
    @Mock
    private IdGenerator idGenerator;
    @Mock
    private KnowledgeAccessService knowledgeAccessService;
    @Mock
    private KnowledgeTagPolicy knowledgeTagPolicy;
    @Mock
    private KnowledgeTagGovernanceService knowledgeTagGovernanceService;
    @Mock
    private KnowledgeSaveSupport knowledgeSaveSupport;
    @InjectMocks
    private KnowledgeDraftServiceImpl service;

    @Test
    void createKnowledgeDefaultsVisibilityToPrivate() {
        when(knowledgeAccessService.requireOrganId()).thenReturn(1L);
        when(knowledgeAccessService.requireUserId()).thenReturn(2L);
        when(idGenerator.generateId()).thenReturn(3L);
        when(knowledgeTagGovernanceService.validateDraftTags(List.of())).thenReturn(List.of());
        when(knowledgeTagPolicy.asJson(List.of())).thenReturn("[]");
        when(knowledgeItemDao.insert(any(KnowledgeItemPo.class))).thenReturn(1);

        KnowledgeCreateDto dto = new KnowledgeCreateDto();
        dto.setKnowledgeKey("imported_document");
        dto.setKnowledgeName("Imported document");
        dto.setKnowledgeType("DOCUMENT");
        dto.setTags(List.of());

        service.createKnowledge(dto);

        ArgumentCaptor<KnowledgeItemPo> itemCaptor = ArgumentCaptor.forClass(KnowledgeItemPo.class);
        verify(knowledgeItemDao).insert(itemCaptor.capture());
        assertEquals("PRIVATE", itemCaptor.getValue().getVisibility());
    }
}
