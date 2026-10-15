package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.dao.KnowledgeTaskDao;
import com.g2rain.knowledge.dao.po.KnowledgeTaskPo;
import com.g2rain.knowledge.dto.KnowledgeTaskCancelDto;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeTaskCommandServiceImplTest {

    @Mock
    private KnowledgeTaskDao knowledgeTaskDao;
    @Mock
    private KnowledgeAccessService knowledgeAccessService;
    @InjectMocks
    private KnowledgeTaskCommandServiceImpl service;

    @Test
    void cancelsPendingTask() {
        KnowledgeTaskPo task = new KnowledgeTaskPo();
        task.setId(9L);
        task.setOrganId(1L);
        task.setVersion(3);
        task.setTaskStatus("PENDING");
        when(knowledgeTaskDao.selectById(9L)).thenReturn(task);
        when(knowledgeTaskDao.cancelPending(eq(1L), eq(9L), eq(3), any(), any())).thenReturn(1);

        KnowledgeTaskCancelDto dto = new KnowledgeTaskCancelDto();
        dto.setTaskId(9L);
        dto.setExpectedVersion(3);
        service.cancel(dto);

        verify(knowledgeAccessService).assertSameOrgan(1L);
        verify(knowledgeTaskDao).cancelPending(eq(1L), eq(9L), eq(3), any(), any());
    }

    @Test
    void rejectsCancelWhenNotPending() {
        KnowledgeTaskPo task = new KnowledgeTaskPo();
        task.setId(9L);
        task.setOrganId(1L);
        task.setVersion(3);
        task.setTaskStatus("RUNNING");
        when(knowledgeTaskDao.selectById(9L)).thenReturn(task);

        KnowledgeTaskCancelDto dto = new KnowledgeTaskCancelDto();
        dto.setTaskId(9L);
        dto.setExpectedVersion(3);
        BusinessException exception = assertThrows(BusinessException.class, () -> service.cancel(dto));
        assertEquals(KnowledgeErrorCode.TASK_NOT_CANCELLABLE.code(), exception.getErrorCode());
    }
}
