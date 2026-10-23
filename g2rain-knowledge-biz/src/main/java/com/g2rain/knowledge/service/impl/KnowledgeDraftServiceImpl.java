package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.id.IdGenerator;
import com.g2rain.common.utils.Moments;
import com.g2rain.knowledge.client.storage.KnowledgeStorageClient;
import com.g2rain.knowledge.client.storage.SourceMetadata;
import com.g2rain.knowledge.client.storage.StoredSource;
import com.g2rain.knowledge.dao.KnowledgeItemDao;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.po.KnowledgeItemPo;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.domain.KnowledgeRevisionStateMachine;
import com.g2rain.knowledge.domain.KnowledgeTagPolicy;
import com.g2rain.knowledge.dto.KnowledgeCreateDto;
import com.g2rain.knowledge.dto.KnowledgeDraftCreateDto;
import com.g2rain.knowledge.dto.KnowledgeDraftEditDto;
import com.g2rain.knowledge.dto.KnowledgeFaqEditDto;
import com.g2rain.knowledge.enums.KnowledgeEnablementStatus;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.enums.KnowledgeRevisionStatus;
import com.g2rain.knowledge.enums.KnowledgeSourceType;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeDraftService;
import com.g2rain.knowledge.service.KnowledgeDraftTxService;
import com.g2rain.knowledge.service.KnowledgeSaveSupport;
import com.g2rain.knowledge.service.KnowledgeTagGovernanceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain commands layered over the generator-created persistence contracts.
 */
@Service
public class KnowledgeDraftServiceImpl implements KnowledgeDraftService {
    private static final String PENDING = "PENDING";

    @Resource
    private KnowledgeItemDao knowledgeItemDao;
    @Resource
    private KnowledgeRevisionDao knowledgeRevisionDao;
    @Resource
    private IdGenerator idGenerator;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;
    @Resource
    private KnowledgeRevisionStateMachine revisionStateMachine;
    @Resource
    private KnowledgeTagPolicy knowledgeTagPolicy;
    @Resource
    private KnowledgeTagGovernanceService knowledgeTagGovernanceService;
    @Resource
    private KnowledgeStorageClient knowledgeStorageClient;
    @Resource
    private KnowledgeDraftTxService knowledgeDraftTxService;
    @Resource
    private KnowledgeSaveSupport knowledgeSaveSupport;

    @Override
    public Long createKnowledge(KnowledgeCreateDto dto) {
        Long organId = knowledgeAccessService.requireOrganId();
        knowledgeSaveSupport.validateCategory(organId, dto.getCategoryId());
        LocalDateTime now = Moments.now();
        KnowledgeItemPo item = new KnowledgeItemPo();
        item.setId(idGenerator.generateId());
        item.setOrganId(organId);
        item.setCategoryId(dto.getCategoryId());
        item.setKnowledgeKey(dto.getKnowledgeKey());
        item.setKnowledgeName(dto.getKnowledgeName());
        item.setKnowledgeType(dto.getKnowledgeType());
        item.setStatus(KnowledgeEnablementStatus.ACTIVE.name());
        item.setDescription(dto.getDescription());
        item.setPriority(Objects.requireNonNullElse(dto.getPriority(), 0));
        item.setTagsJson(knowledgeTagPolicy.asJson(knowledgeTagGovernanceService.validateDraftTags(dto.getTags())));
        item.setCreateUserId(knowledgeAccessService.requireUserId());
        item.setCreateTime(now);
        item.setUpdateTime(now);
        requireInserted(knowledgeItemDao.insert(item));

        return item.getId();
    }

    @Override
    @Transactional
    public Long createDraft(KnowledgeDraftCreateDto dto) {
        Long organId = knowledgeAccessService.requireOrganId();
        KnowledgeItemPo item = knowledgeSaveSupport.requireActiveItem(dto.getKnowledgeId(), organId);
        knowledgeSaveSupport.assertCompatibleSource(item.getKnowledgeType(), dto.getSourceType());

        LocalDateTime now = Moments.now();
        KnowledgeRevisionPo revision = new KnowledgeRevisionPo();
        revision.setId(idGenerator.generateId());
        revision.setOrganId(organId);
        revision.setKnowledgeId(item.getId());
        revision.setRevisionNo(dto.getRevisionNo());
        revision.setSourceType(dto.getSourceType());
        revision.setRevisionStatus(KnowledgeRevisionStatus.DRAFT.name());
        revision.setChunkCount(0);
        revision.setIndexStatus(PENDING);
        revision.setCreateUserId(knowledgeAccessService.requireUserId());
        revision.setCreateTime(now);
        revision.setUpdateTime(now);
        requireInserted(knowledgeRevisionDao.insert(revision));
        return revision.getId();
    }

    @Override
    @Transactional
    public void editDraft(KnowledgeDraftEditDto dto) {
        KnowledgeRevisionPo current = requireRevision(dto.getRevisionId());
        revisionStateMachine.requireEditable(current.getRevisionStatus());
        assertExpectedVersion(current, dto.getExpectedVersion());

        KnowledgeRevisionPo update = new KnowledgeRevisionPo();
        update.setId(current.getId());
        update.setVersion(dto.getExpectedVersion());
        update.setUpdateTime(Moments.now());
        if (knowledgeRevisionDao.updateByVersion(update) != 1) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_VERSION_CONFLICT);
        }
    }

    @Override
    @Transactional
    public void editFaq(KnowledgeFaqEditDto dto) {
        KnowledgeRevisionPo current = requireRevision(dto.getRevisionId());
        revisionStateMachine.requireEditable(current.getRevisionStatus());
        assertExpectedVersion(current, dto.getExpectedVersion());
        if (!KnowledgeSourceType.FAQ.name().equals(current.getSourceType())) {
            throw new BusinessException(KnowledgeErrorCode.SOURCE_TYPE_MISMATCH);
        }

        KnowledgeRevisionPo update = new KnowledgeRevisionPo();
        update.setId(current.getId());
        update.setVersion(dto.getExpectedVersion());
        update.setFaqQuestion(dto.getQuestion());
        update.setFaqAnswer(dto.getAnswer());
        update.setUpdateTime(Moments.now());
        if (knowledgeRevisionDao.updateByVersion(update) != 1) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_VERSION_CONFLICT);
        }
    }

    @Override
    public void replaceFile(Long revisionId, Integer expectedVersion, InputStream content, SourceMetadata metadata) {
        KnowledgeRevisionPo current = requireRevision(revisionId);
        revisionStateMachine.requireEditable(current.getRevisionStatus());
        assertExpectedVersion(current, expectedVersion);
        if (!KnowledgeSourceType.FILE.name().equals(current.getSourceType())) {
            throw new BusinessException(KnowledgeErrorCode.SOURCE_TYPE_MISMATCH);
        }

        String previousPath = current.getSourceStoragePath();
        StoredSource stored = knowledgeStorageClient.storeSource(
            current.getOrganId(), current.getKnowledgeId(), revisionId, content, metadata);
        try {
            knowledgeDraftTxService.attachFile(revisionId, expectedVersion, stored);
        } catch (RuntimeException exception) {
            knowledgeStorageClient.deleteUnreferenced(stored.relativePath());
            throw exception;
        }
        if (Objects.nonNull(previousPath) && !previousPath.isBlank()
            && !Objects.equals(previousPath, stored.relativePath())) {
            knowledgeStorageClient.deleteUnreferenced(previousPath);
        }
    }

    private KnowledgeRevisionPo requireRevision(Long revisionId) {
        KnowledgeRevisionPo revision = knowledgeRevisionDao.selectById(revisionId);
        if (Objects.isNull(revision)) {
            throw new BusinessException(SystemErrorCode.DATA_NOT_EXISTS, revisionId);
        }
        knowledgeAccessService.assertSameOrgan(revision.getOrganId());
        return revision;
    }

    private void assertExpectedVersion(KnowledgeRevisionPo revision, Integer expectedVersion) {
        if (!Objects.equals(revision.getVersion(), expectedVersion)) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_VERSION_CONFLICT);
        }
    }

    private void requireInserted(int affectedRows) {
        if (affectedRows != 1) {
            throw new BusinessException(SystemErrorCode.CREATE_DATA_ERROR);
        }
    }
}
