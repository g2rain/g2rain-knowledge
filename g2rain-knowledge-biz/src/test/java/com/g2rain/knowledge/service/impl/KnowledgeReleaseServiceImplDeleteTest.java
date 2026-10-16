package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.dao.KnowledgeReleaseDao;
import com.g2rain.knowledge.dao.po.KnowledgeReleasePo;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeReleaseServiceImplDeleteTest {

    @Mock
    private KnowledgeReleaseDao knowledgeReleaseDao;
    @Mock
    private KnowledgeAccessService knowledgeAccessService;
    @InjectMocks
    private KnowledgeReleaseServiceImpl service;

    @Test
    void deletesUnpublishedReleaseWithoutActiveRevision() {
        KnowledgeReleasePo release = release("UNPUBLISHED", null);
        when(knowledgeReleaseDao.selectById(7L)).thenReturn(release);
        when(knowledgeReleaseDao.delete(7L)).thenReturn(1);

        assertEquals(1, service.delete(7L));

        verify(knowledgeAccessService).assertSameOrgan(1L);
        verify(knowledgeReleaseDao).delete(7L);
    }

    @Test
    void rejectsPublishedRelease() {
        when(knowledgeReleaseDao.selectById(7L)).thenReturn(release("PUBLISHED", 20L));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.delete(7L));

        assertEquals(KnowledgeErrorCode.RELEASE_DELETE_FORBIDDEN.code(), exception.getErrorCode());
        verify(knowledgeReleaseDao, never()).delete(7L);
    }

    @Test
    void rejectsUnpublishedReleaseThatStillPointsAtAnActiveRevision() {
        when(knowledgeReleaseDao.selectById(7L)).thenReturn(release("UNPUBLISHED", 20L));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.delete(7L));

        assertEquals(KnowledgeErrorCode.RELEASE_DELETE_FORBIDDEN.code(), exception.getErrorCode());
        verify(knowledgeReleaseDao, never()).delete(7L);
    }

    private static KnowledgeReleasePo release(String status, Long activeRevisionId) {
        KnowledgeReleasePo release = new KnowledgeReleasePo();
        release.setId(7L);
        release.setOrganId(1L);
        release.setReleaseStatus(status);
        release.setActiveRevisionId(activeRevisionId);
        return release;
    }
}
