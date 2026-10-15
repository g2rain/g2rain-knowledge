package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.utils.Moments;
import com.g2rain.knowledge.dao.KnowledgeTaskDao;
import com.g2rain.knowledge.dao.po.KnowledgeTaskPo;
import com.g2rain.knowledge.dto.KnowledgeTaskCancelDto;
import com.g2rain.knowledge.dto.KnowledgeTaskRetryDto;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeTaskCommandService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Retrying preserves the original task identity and idempotency key. Cancelling
 * is intentionally limited to PENDING until persistent cooperative-cancel
 * metadata is introduced for running tasks.
 */
@Service
public class KnowledgeTaskCommandServiceImpl implements KnowledgeTaskCommandService {
    private static final String FAILED = "FAILED";
    private static final String PENDING = "PENDING";

    @Resource
    private KnowledgeTaskDao knowledgeTaskDao;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;

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
    }

    private KnowledgeTaskPo requireTask(Long taskId, Integer expectedVersion) {
        KnowledgeTaskPo task = knowledgeTaskDao.selectById(taskId);
        if (Objects.isNull(task)) {
            throw new BusinessException(SystemErrorCode.DATA_NOT_EXISTS, taskId);
        }
        knowledgeAccessService.assertSameOrgan(task.getOrganId());
        if (!Objects.equals(task.getVersion(), expectedVersion)) {
            throw new BusinessException(KnowledgeErrorCode.TASK_VERSION_CONFLICT);
        }
        return task;
    }
}
