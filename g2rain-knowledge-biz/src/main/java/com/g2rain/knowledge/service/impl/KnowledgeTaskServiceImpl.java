package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.id.IdGenerator;
import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.utils.Asserts;
import com.g2rain.common.utils.Moments;
import com.g2rain.common.validation.Validations;
import com.g2rain.knowledge.converter.KnowledgeTaskConverter;
import com.g2rain.knowledge.dao.KnowledgeItemDao;
import com.g2rain.knowledge.dao.KnowledgeTaskDao;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.po.KnowledgeItemPo;
import com.g2rain.knowledge.dao.po.KnowledgeTaskPo;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.dto.KnowledgeTaskDto;
import com.g2rain.knowledge.dto.KnowledgeTaskSelectDto;
import com.g2rain.knowledge.service.KnowledgeTaskService;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.enums.KnowledgeRevisionStatus;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.knowledge.task.KnowledgeTaskRequestedEvent;
import com.g2rain.knowledge.task.KnowledgeTaskSchedule;
import org.springframework.context.ApplicationEventPublisher;
import com.g2rain.knowledge.vo.KnowledgeTaskVo;
import com.g2rain.mybatis.pagination.PageContext;
import com.g2rain.mybatis.pagination.model.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 知识解析与索引处理任务表服务实现类
 * 表名: knowledge_task
 *
 * @author G2rain Generator
 */
@Slf4j
@Service(value = "knowledgeTaskServiceImpl")
public class KnowledgeTaskServiceImpl implements KnowledgeTaskService {
    @Resource(name = "knowledgeTaskDao")
    private KnowledgeTaskDao knowledgeTaskDao;
    @Resource(name = "knowledgeRevisionDao")
    private KnowledgeRevisionDao knowledgeRevisionDao;
    @Resource(name = "knowledgeItemDao")
    private KnowledgeItemDao knowledgeItemDao;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    private IdGenerator idGenerator;

    @Qualifier("idGenerator")
    @Autowired(required = false)
    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public List<KnowledgeTaskVo> selectList(KnowledgeTaskSelectDto selectDto) {
        return knowledgeTaskDao.selectList(selectDto).stream().map(KnowledgeTaskConverter.INSTANCE::po2vo).toList();
    }

    @Override
    public PageData<KnowledgeTaskVo> selectPage(PageSelectListDto<KnowledgeTaskSelectDto> selectDto) {
        Page<KnowledgeTaskPo> page = PageContext.of(selectDto.getPageNum(), selectDto.getPageSize(),
            () -> knowledgeTaskDao.selectList(selectDto.getQuery()));
        List<KnowledgeTaskVo> result = page.getResult().stream().map(KnowledgeTaskConverter.INSTANCE::po2vo).toList();
        return PageData.of(page.getPageNum(), page.getPageSize(), page.getTotal(), result);
    }

    @Override
    public Long save(KnowledgeTaskDto dto) {
        Long organId = knowledgeAccessService.requireOrganId();
        Long id = dto.getId();
        boolean creating = Objects.isNull(id) || id == 0;
        if (creating) {
            dto.setOrganId(organId);
            Validations.validateSave(dto);
            KnowledgeTaskPo entity = KnowledgeTaskConverter.INSTANCE.dto2po(dto);
            entity.setId(idGenerator.generateId());
            entity.setOrganId(organId);
            LocalDateTime now = Moments.now();
            entity.setUpdateTime(now);
            entity.setCreateTime(now);
            Asserts.greaterThan(knowledgeTaskDao.insert(entity), 0, SystemErrorCode.CREATE_DATA_ERROR);
            return entity.getId();
        }
        KnowledgeTaskPo existing = knowledgeTaskDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        knowledgeAccessService.assertSameOrgan(existing.getOrganId());
        Validations.validateSave(dto);
        KnowledgeTaskPo entity = KnowledgeTaskConverter.INSTANCE.dto2po(dto);
        entity.setId(existing.getId());
        entity.setOrganId(existing.getOrganId());
        entity.setCreateTime(existing.getCreateTime());
        entity.setUpdateTime(Moments.now());
        Asserts.greaterThan(knowledgeTaskDao.update(entity), 0, SystemErrorCode.UPDATE_DATA_ERROR, id);
        return entity.getId();
    }

    @Override
    public int delete(Long id) {
        KnowledgeTaskPo existing = knowledgeTaskDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        knowledgeAccessService.assertSameOrgan(existing.getOrganId());
        return knowledgeTaskDao.delete(id);
    }

    @Override
    @Transactional
    public Long processRevision(Long revisionId) {
        KnowledgeRevisionPo revision = knowledgeRevisionDao.selectById(revisionId);
        Asserts.isTrue(Objects.nonNull(revision), SystemErrorCode.DATA_NOT_EXISTS, revisionId);
        knowledgeAccessService.assertSameOrgan(revision.getOrganId());
        if (!KnowledgeRevisionStatus.APPROVED.name().equals(revision.getRevisionStatus())) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_NOT_REVIEWABLE);
        }
        KnowledgeItemPo item = knowledgeItemDao.selectById(revision.getKnowledgeId());
        String tagsJson = item == null ? null : item.getTagsJson();
        String inputVersion = revision.getSourceSha256() == null
            ? Integer.toHexString(Objects.hash(revision.getFaqQuestion(), revision.getFaqAnswer(), tagsJson))
            : revision.getSourceSha256() + ':' + Integer.toHexString(Objects.hashCode(tagsJson));
        String key = "process:" + revisionId + ':' + inputVersion;
        KnowledgeTaskSelectDto query = new KnowledgeTaskSelectDto();
        query.setOrganId(knowledgeAccessService.requireOrganId());
        query.setIdempotencyKey(key);
        List<KnowledgeTaskPo> existing = knowledgeTaskDao.selectList(query);
        if (!existing.isEmpty()) {
            KnowledgeTaskPo found = existing.getFirst();
            log.info("processRevision reuse existing task: revisionId={}, taskId={}, taskStatus={}, idempotencyKey={}",
                revisionId, found.getId(), found.getTaskStatus(), key);
            return found.getId();
        }
        LocalDateTime now = Moments.now();
        KnowledgeRevisionPo update = new KnowledgeRevisionPo();
        update.setId(revisionId);
        update.setVersion(revision.getVersion());
        update.setRevisionStatus(KnowledgeRevisionStatus.PROCESSING.name());
        update.setUpdateTime(now);
        if (knowledgeRevisionDao.updateByVersion(update) != 1) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_VERSION_CONFLICT);
        }
        KnowledgeTaskPo task = new KnowledgeTaskPo();
        task.setId(idGenerator.generateId());
        task.setOrganId(revision.getOrganId());
        task.setRevisionId(revisionId);
        task.setTaskType("PROCESS");
        task.setTaskStatus("PENDING");
        task.setIdempotencyKey(key);
        task.setAttemptNo(0);
        task.setAvailableTime(KnowledgeTaskSchedule.immediate());
        task.setTraceId(PrincipalContextHolder.getTraceId());
        task.setCreateTime(now);
        task.setUpdateTime(now);
        Asserts.greaterThan(knowledgeTaskDao.insert(task), 0, SystemErrorCode.CREATE_DATA_ERROR);
        applicationEventPublisher.publishEvent(new KnowledgeTaskRequestedEvent(task.getId()));
        log.info("processRevision created PENDING task and published event: revisionId={}, taskId={}, organId={}, availableTime={}, idempotencyKey={}",
            revisionId, task.getId(), task.getOrganId(), task.getAvailableTime(), key);
        return task.getId();
    }
}
