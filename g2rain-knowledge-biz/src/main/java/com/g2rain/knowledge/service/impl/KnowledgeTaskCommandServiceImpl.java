package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.utils.Moments;
import com.g2rain.knowledge.dao.KnowledgeTaskDao;
import com.g2rain.knowledge.dao.po.KnowledgeTaskPo;
import com.g2rain.knowledge.dto.KnowledgeTaskCancelDto;
import com.g2rain.knowledge.dto.KnowledgeTaskDispatchDto;
import com.g2rain.knowledge.dto.KnowledgeTaskRetryDto;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeTaskCommandService;
import com.g2rain.knowledge.task.KnowledgeTaskRequestedEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Retrying preserves the original task identity and idempotency key. Cancelling
 * is intentionally limited to PENDING until persistent cooperative-cancel
 * metadata is introduced for running tasks. Dispatch re-arms PENDING tasks and
 * republishes the processing event when the original AFTER_COMMIT listener was missed.
 */
@Slf4j
@Service
public class KnowledgeTaskCommandServiceImpl implements KnowledgeTaskCommandService {
    private static final String FAILED = "FAILED";
    private static final String PENDING = "PENDING";

    @Resource
    private KnowledgeTaskDao knowledgeTaskDao;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void retry(KnowledgeTaskRetryDto dto) {
        KnowledgeTaskPo task = requireTask(dto.getTaskId(), dto.getExpectedVersion());
        if (!FAILED.equals(task.getTaskStatus())) {
            throw new BusinessException(KnowledgeErrorCode.TASK_NOT_RETRYABLE);
        }
        LocalDateTime now = Moments.now();
        if (knowledgeTaskDao.retryFailed(task.getOrganId(), task.getId(), dto.getExpectedVersion(), now, now) != 1) {
            throw new BusinessException(KnowledgeErrorCode.TASK_VERSION_CONFLICT);
        }
        applicationEventPublisher.publishEvent(new KnowledgeTaskRequestedEvent(task.getId()));
        log.info("retry set task back to PENDING and published event: taskId={}, revisionId={}, organId={}",
            task.getId(), task.getRevisionId(), task.getOrganId());
    }

    @Override
    @Transactional
    public void cancel(KnowledgeTaskCancelDto dto) {
        KnowledgeTaskPo task = requireTask(dto.getTaskId(), dto.getExpectedVersion());
        if (!PENDING.equals(task.getTaskStatus())) {
            throw new BusinessException(KnowledgeErrorCode.TASK_NOT_CANCELLABLE);
        }
        LocalDateTime now = Moments.now();
        if (knowledgeTaskDao.cancelPending(task.getOrganId(), task.getId(), dto.getExpectedVersion(), now, now) != 1) {
            throw new BusinessException(KnowledgeErrorCode.TASK_VERSION_CONFLICT);
        }
        log.info("canceled PENDING task: taskId={}, revisionId={}, organId={}",
            task.getId(), task.getRevisionId(), task.getOrganId());
    }

    @Override
    @Transactional
    public void dispatch(KnowledgeTaskDispatchDto dto) {
        KnowledgeTaskPo task = requireTask(dto.getTaskId());
        if (!PENDING.equals(task.getTaskStatus())) {
            throw new BusinessException(KnowledgeErrorCode.TASK_NOT_DISPATCHABLE);
        }
        LocalDateTime now = Moments.now();
        if (knowledgeTaskDao.armPending(task.getOrganId(), task.getId(), task.getVersion(), now, now) != 1) {
            throw new BusinessException(KnowledgeErrorCode.TASK_VERSION_CONFLICT);
        }
        applicationEventPublisher.publishEvent(new KnowledgeTaskRequestedEvent(task.getId()));
        log.info("dispatch armed PENDING task and published event: taskId={}, revisionId={}, organId={}",
            task.getId(), task.getRevisionId(), task.getOrganId());
    }

    private KnowledgeTaskPo requireTask(Long taskId) {
        KnowledgeTaskPo task = knowledgeTaskDao.selectById(taskId);
        if (Objects.isNull(task)) {
            throw new BusinessException(SystemErrorCode.DATA_NOT_EXISTS, taskId);
        }
        knowledgeAccessService.assertSameOrgan(task.getOrganId());
        return task;
    }

    private KnowledgeTaskPo requireTask(Long taskId, Integer expectedVersion) {
        KnowledgeTaskPo task = requireTask(taskId);
        if (!Objects.equals(task.getVersion(), expectedVersion)) {
            throw new BusinessException(KnowledgeErrorCode.TASK_VERSION_CONFLICT);
        }
        return task;
    }
}
