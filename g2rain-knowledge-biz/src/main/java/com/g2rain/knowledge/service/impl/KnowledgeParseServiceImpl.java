package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.id.IdGenerator;
import com.g2rain.common.utils.Moments;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.KnowledgeTaskDao;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.dao.po.KnowledgeTaskPo;
import com.g2rain.knowledge.domain.KnowledgeRevisionStateMachine;
import com.g2rain.knowledge.dto.KnowledgeParseDto;
import com.g2rain.knowledge.dto.KnowledgeTaskSelectDto;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.enums.KnowledgeRevisionStatus;
import com.g2rain.knowledge.enums.KnowledgeSourceType;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeParseService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Persists the state transition and task in the same local transaction.
 */
@Service
public class KnowledgeParseServiceImpl implements KnowledgeParseService {
    private static final String PARSE = "PARSE";
    private static final String PENDING = "PENDING";

    @Resource
    private KnowledgeRevisionDao knowledgeRevisionDao;
    @Resource
    private KnowledgeTaskDao knowledgeTaskDao;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;
    @Resource
    private KnowledgeRevisionStateMachine revisionStateMachine;
    @Resource
    private IdGenerator idGenerator;

    @Override
    @Transactional
    public Long submitParse(KnowledgeParseDto dto) {
        Long organId = knowledgeAccessService.requireOrganId();
        KnowledgeRevisionPo revision = knowledgeRevisionDao.selectById(dto.getRevisionId());
        if (Objects.isNull(revision)) {
            throw new BusinessException(SystemErrorCode.DATA_NOT_EXISTS, dto.getRevisionId());
        }
        knowledgeAccessService.assertSameOrgan(revision.getOrganId());
        revisionStateMachine.requireEditable(revision.getRevisionStatus());
        if (!Objects.equals(revision.getVersion(), dto.getExpectedVersion())) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_VERSION_CONFLICT);
        }
        assertSourceReady(revision);

        String idempotencyKey = parseIdempotencyKey(revision);
        KnowledgeTaskSelectDto query = new KnowledgeTaskSelectDto();
        query.setOrganId(organId);
        query.setIdempotencyKey(idempotencyKey);
        List<KnowledgeTaskPo> tasks = knowledgeTaskDao.selectList(query);
        if (!tasks.isEmpty()) {
            return tasks.getFirst().getId();
        }

        LocalDateTime now = Moments.now();
        KnowledgeRevisionPo update = new KnowledgeRevisionPo();
        update.setId(revision.getId());
        update.setVersion(dto.getExpectedVersion());
        update.setRevisionStatus(KnowledgeRevisionStatus.PARSING.name());
        update.setUpdateTime(now);
        if (knowledgeRevisionDao.updateByVersion(update) != 1) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_VERSION_CONFLICT);
        }

        KnowledgeTaskPo task = new KnowledgeTaskPo();
        task.setId(idGenerator.generateId());
        task.setOrganId(organId);
        task.setRevisionId(revision.getId());
        task.setTaskType(PARSE);
        task.setTaskStatus(PENDING);
        task.setIdempotencyKey(idempotencyKey);
        task.setAttemptNo(0);
        task.setAvailableTime(now);
        task.setTraceId(PrincipalContextHolder.getTraceId());
        task.setCreateTime(now);
        task.setUpdateTime(now);
        if (knowledgeTaskDao.insert(task) != 1) {
            throw new BusinessException(SystemErrorCode.CREATE_DATA_ERROR);
        }
        return task.getId();
    }

    private void assertSourceReady(KnowledgeRevisionPo revision) {
        boolean ready = KnowledgeSourceType.FILE.name().equals(revision.getSourceType())
            ? isNotBlank(revision.getSourceStoragePath()) && isNotBlank(revision.getSourceSha256())
            : isNotBlank(revision.getFaqQuestion()) && isNotBlank(revision.getFaqAnswer());
        if (!ready) {
            throw new BusinessException(KnowledgeErrorCode.SOURCE_NOT_READY);
        }
    }

    private String parseIdempotencyKey(KnowledgeRevisionPo revision) {
        String inputVersion = KnowledgeSourceType.FILE.name().equals(revision.getSourceType())
            ? revision.getSourceSha256() : Integer.toHexString(Objects.hash(revision.getFaqQuestion(), revision.getFaqAnswer()));
        return "parse:" + revision.getId() + ':' + inputVersion;
    }

    private boolean isNotBlank(String value) {
        return Objects.nonNull(value) && !value.isBlank();
    }
}
