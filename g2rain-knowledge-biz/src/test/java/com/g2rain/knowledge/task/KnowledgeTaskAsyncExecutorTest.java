package com.g2rain.knowledge.task;

import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.KnowledgeTaskDao;
import com.g2rain.knowledge.dao.po.KnowledgeTaskPo;
import com.g2rain.knowledge.service.KnowledgeIndexService;
import com.g2rain.knowledge.service.KnowledgeProcessPersistenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeTaskAsyncExecutorTest {

    @Mock
    private KnowledgeTaskDao knowledgeTaskDao;
    @Mock
    private KnowledgeRevisionDao knowledgeRevisionDao;
    @Mock
    private KnowledgeProcessPersistenceService knowledgeProcessPersistenceService;
    @Mock
    private KnowledgeIndexService knowledgeIndexService;
    @InjectMocks
    private KnowledgeTaskAsyncExecutor executor;

    @Test
    void claimsTaskInsidePersistedOrganScope() {
        when(knowledgeTaskDao.selectByIdWithoutIsolation(8L)).thenReturn(task(1L));
        when(knowledgeTaskDao.selectById(8L)).thenReturn(task(1L));
        when(knowledgeTaskDao.claim(eq(1L), eq(8L), eq(2), any(), any(), any())).thenReturn(1);

        executor.execute(new KnowledgeTaskRequestedEvent(8L));

        verify(knowledgeTaskDao).claim(eq(1L), eq(8L), eq(2), any(), any(), any());
        verify(knowledgeRevisionDao, never()).selectById(any());
    }

    @Test
    void skipsClaimWhenIsolatedReadDoesNotMatchPersistedOrgan() {
        when(knowledgeTaskDao.selectByIdWithoutIsolation(8L)).thenReturn(task(1L));
        when(knowledgeTaskDao.selectById(8L)).thenReturn(task(9L));

        executor.execute(new KnowledgeTaskRequestedEvent(8L));

        verify(knowledgeTaskDao, never()).claim(any(), any(), any(), any(), any(), any());
    }

    private static KnowledgeTaskPo task(Long organId) {
        KnowledgeTaskPo task = new KnowledgeTaskPo();
        task.setId(8L);
        task.setOrganId(organId);
        task.setVersion(2);
        task.setTaskType("INDEX");
        return task;
    }
}
