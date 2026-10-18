package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.dao.KnowledgeTaskDao;
import com.g2rain.knowledge.dao.po.KnowledgeTaskPo;
import com.g2rain.knowledge.dto.KnowledgeTaskCancelDto;
import com.g2rain.knowledge.dto.KnowledgeTaskDispatchDto;
import com.g2rain.knowledge.dto.KnowledgeTaskRetryDto;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.task.KnowledgeTaskRequestedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
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

    @Test
    void dispatchesPendingTaskAndPublishesEvent() {
        KnowledgeTaskPo task = new KnowledgeTaskPo();
        task.setId(9L);
        task.setOrganId(1L);
        task.setRevisionId(4L);
        task.setVersion(3);
        task.setTaskStatus("PENDING");
        when(knowledgeTaskDao.selectById(9L)).thenReturn(task);
        when(knowledgeTaskDao.armPending(eq(1L), eq(9L), eq(3), any(), any())).thenReturn(1);

        KnowledgeTaskDispatchDto dto = new KnowledgeTaskDispatchDto();
        dto.setTaskId(9L);
        service.dispatch(dto);

        verify(knowledgeTaskDao).armPending(eq(1L), eq(9L), eq(3), any(), any());
        ArgumentCaptor<KnowledgeTaskRequestedEvent> eventCaptor = ArgumentCaptor.forClass(KnowledgeTaskRequestedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(9L, eventCaptor.getValue().taskId());
    }

    @Test
    void rejectsDispatchWhenNotPending() {
        KnowledgeTaskPo task = new KnowledgeTaskPo();
        task.setId(9L);
        task.setOrganId(1L);
        task.setVersion(3);
        task.setTaskStatus("FAILED");
        when(knowledgeTaskDao.selectById(9L)).thenReturn(task);

        KnowledgeTaskDispatchDto dto = new KnowledgeTaskDispatchDto();
        dto.setTaskId(9L);
        BusinessException exception = assertThrows(BusinessException.class, () -> service.dispatch(dto));
        assertEquals(KnowledgeErrorCode.TASK_NOT_DISPATCHABLE.code(), exception.getErrorCode());
    }

    @Test
    void retryPublishesEvent() {
        KnowledgeTaskPo task = new KnowledgeTaskPo();
        task.setId(9L);
        task.setOrganId(1L);
        task.setRevisionId(4L);
        task.setVersion(3);
        task.setTaskStatus("FAILED");
        when(knowledgeTaskDao.selectById(9L)).thenReturn(task);
        when(knowledgeTaskDao.retryFailed(eq(1L), eq(9L), eq(3), any(), any())).thenReturn(1);

        KnowledgeTaskRetryDto dto = new KnowledgeTaskRetryDto();
        dto.setTaskId(9L);
        dto.setExpectedVersion(3);
        service.retry(dto);

        ArgumentCaptor<KnowledgeTaskRequestedEvent> eventCaptor = ArgumentCaptor.forClass(KnowledgeTaskRequestedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(9L, eventCaptor.getValue().taskId());
    }
}
