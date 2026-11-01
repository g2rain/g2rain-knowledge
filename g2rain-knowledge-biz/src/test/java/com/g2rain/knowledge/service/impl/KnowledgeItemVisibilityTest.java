package com.g2rain.knowledge.service.impl;

import com.g2rain.knowledge.dao.KnowledgeItemDao;
import com.g2rain.knowledge.dao.po.KnowledgeItemPo;
import com.g2rain.knowledge.domain.KnowledgePublicWriteGuard;
import com.g2rain.knowledge.dto.KnowledgeVisibilityDto;
import com.g2rain.knowledge.enums.KnowledgeVisibility;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeVisibilitySyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeItemVisibilityTest {

    @Mock
    private KnowledgeItemDao knowledgeItemDao;
    @Mock
    private KnowledgeAccessService knowledgeAccessService;
    @Mock
    private KnowledgePublicWriteGuard knowledgePublicWriteGuard;
    @Mock
    private KnowledgeVisibilitySyncService knowledgeVisibilitySyncService;
    @InjectMocks
    private KnowledgeItemServiceImpl service;

    @Test
    void setVisibilityUsesCurrentVersionWhenLegacyClientOmitsIt() {
        KnowledgeItemPo existing = new KnowledgeItemPo();
        existing.setId(31504L);
        existing.setOrganId(2L);
        existing.setVisibility("PRIVATE");
        existing.setVersion(4);
        when(knowledgeItemDao.selectById(31504L)).thenReturn(existing);
        when(knowledgeItemDao.updateByVersion(any(KnowledgeItemPo.class))).thenReturn(1);
        when(knowledgePublicWriteGuard.requireValid("PUBLIC")).thenReturn(KnowledgeVisibility.PUBLIC);

        KnowledgeVisibilityDto dto = new KnowledgeVisibilityDto();
        dto.setKnowledgeId(31504L);
        dto.setVisibility("PUBLIC");

        service.setVisibility(dto);

        ArgumentCaptor<KnowledgeItemPo> updateCaptor = ArgumentCaptor.forClass(KnowledgeItemPo.class);
        verify(knowledgeItemDao).updateByVersion(updateCaptor.capture());
        assertEquals(4, updateCaptor.getValue().getVersion());
        assertEquals("PUBLIC", updateCaptor.getValue().getVisibility());
        verify(knowledgeVisibilitySyncService).syncActiveRevision(31504L);
    }
}
