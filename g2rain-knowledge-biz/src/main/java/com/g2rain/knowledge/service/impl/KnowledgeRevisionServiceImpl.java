package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.id.IdGenerator;
import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.utils.Asserts;
import com.g2rain.common.utils.Moments;
import com.g2rain.common.validation.Validations;
import com.g2rain.knowledge.client.storage.KnowledgeStorageClient;
import com.g2rain.knowledge.converter.KnowledgeRevisionConverter;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.po.KnowledgeItemPo;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.domain.KnowledgeRevisionStateMachine;
import com.g2rain.knowledge.dto.KnowledgeRevisionDto;
import com.g2rain.knowledge.dto.KnowledgeRevisionSelectDto;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.enums.KnowledgeRevisionStatus;
import com.g2rain.knowledge.enums.KnowledgeSourceType;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeRevisionService;
import com.g2rain.knowledge.service.KnowledgeSaveSupport;
import com.g2rain.knowledge.service.source.KnowledgeSourceDownload;
import com.g2rain.knowledge.vo.KnowledgeRevisionVo;
import com.g2rain.mybatis.pagination.PageContext;
import com.g2rain.mybatis.pagination.model.Page;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 知识版本表服务实现类
 * 表名: knowledge_revision
 *
 * @author G2rain Generator
 */
@Service(value = "knowledgeRevisionServiceImpl")
public class KnowledgeRevisionServiceImpl implements KnowledgeRevisionService {

    private static final String PENDING = "PENDING";

    @Resource(name = "knowledgeRevisionDao")
    private KnowledgeRevisionDao knowledgeRevisionDao;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;
    @Resource
    private KnowledgeSaveSupport knowledgeSaveSupport;
    @Resource
    private KnowledgeRevisionStateMachine revisionStateMachine;
    @Resource
    private KnowledgeStorageClient knowledgeStorageClient;

    private IdGenerator idGenerator;

    @Qualifier("idGenerator")
    @Autowired(required = false)
    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public List<KnowledgeRevisionVo> selectList(KnowledgeRevisionSelectDto selectDto) {
        return knowledgeRevisionDao.selectList(selectDto)
            .stream()
            .map(KnowledgeRevisionConverter.INSTANCE::po2vo)
            .toList();
    }

    @Override
    public PageData<KnowledgeRevisionVo> selectPage(PageSelectListDto<KnowledgeRevisionSelectDto> selectDto) {
        Page<KnowledgeRevisionPo> page = PageContext.of(selectDto.getPageNum(), selectDto.getPageSize(), () -> {
            knowledgeRevisionDao.selectList(selectDto.getQuery());
        });
        List<KnowledgeRevisionVo> result = page.getResult()
            .stream()
            .map(KnowledgeRevisionConverter.INSTANCE::po2vo)
            .toList();
        return PageData.of(page.getPageNum(), page.getPageSize(), page.getTotal(), result);
    }

    @Override
    @Transactional
    public Long save(KnowledgeRevisionDto dto) {
        Long organId = knowledgeAccessService.requireOrganId();
        Long id = dto.getId();
        boolean creating = Objects.isNull(id) || id == 0;
        if (creating) {
            return createDraft(dto, organId);
        }
        return updateDraft(dto, organId);
    }

    @Override
    public KnowledgeSourceDownload downloadSource(Long revisionId) {
        knowledgeAccessService.requireOrganId();
        KnowledgeRevisionPo revision = knowledgeRevisionDao.selectById(revisionId);
        Asserts.isTrue(Objects.nonNull(revision), SystemErrorCode.DATA_NOT_EXISTS, revisionId);
        knowledgeAccessService.assertSameOrgan(revision.getOrganId());

        if (!KnowledgeSourceType.FILE.name().equals(revision.getSourceType())) {
            throw new BusinessException(KnowledgeErrorCode.SOURCE_DOWNLOAD_NOT_SUPPORTED);
        }
        if (Objects.isNull(revision.getSourceStoragePath()) || revision.getSourceStoragePath().isBlank()) {
            throw new BusinessException(KnowledgeErrorCode.SOURCE_NOT_READY);
        }

        InputStream content = knowledgeStorageClient.openSource(revision.getSourceStoragePath());
        return new KnowledgeSourceDownload(revision.getSourceFileName(), revision.getSourceContentType(), content);
    }

    @Override
    public int delete(Long id) {
        KnowledgeRevisionPo existing = knowledgeRevisionDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        knowledgeAccessService.assertSameOrgan(existing.getOrganId());
        return knowledgeRevisionDao.delete(id);
    }

    private Long createDraft(KnowledgeRevisionDto dto, Long organId) {
        normalizeCreate(dto, organId);
        Validations.validateSave(dto);

        KnowledgeRevisionPo entity = KnowledgeRevisionConverter.INSTANCE.dto2po(dto);
        entity.setId(idGenerator.generateId());
        entity.setOrganId(organId);
        entity.setRevisionStatus(KnowledgeRevisionStatus.DRAFT.name());
        entity.setChunkCount(0);
        entity.setIndexStatus(PENDING);
        entity.setCreateUserId(dto.getCreateUserId());
        clearProtectedSystemFields(entity);
        if (KnowledgeSourceType.FILE.name().equals(entity.getSourceType())) {
            entity.setFaqQuestion(null);
            entity.setFaqAnswer(null);
        } else if (KnowledgeSourceType.FAQ.name().equals(entity.getSourceType())) {
            clearFileMetadata(entity);
        }
        LocalDateTime now = Moments.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        Asserts.greaterThan(knowledgeRevisionDao.insert(entity), 0, SystemErrorCode.CREATE_DATA_ERROR);
        return entity.getId();
    }

    private Long updateDraft(KnowledgeRevisionDto dto, Long organId) {
        KnowledgeRevisionPo existing = knowledgeRevisionDao.selectById(dto.getId());
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, dto.getId());
        knowledgeAccessService.assertSameOrgan(existing.getOrganId());
        revisionStateMachine.requireEditable(existing.getRevisionStatus());
        if (Objects.isNull(dto.getVersion())) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_VERSION_CONFLICT);
        }
        if (!Objects.equals(existing.getVersion(), dto.getVersion())) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_VERSION_CONFLICT);
        }
        Validations.validateSave(dto);

        KnowledgeRevisionPo update = buildDraftUpdate(dto, existing);
        if (knowledgeRevisionDao.updateByVersion(update) != 1) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_VERSION_CONFLICT);
        }
        return existing.getId();
    }

    private void normalizeCreate(KnowledgeRevisionDto dto, Long organId) {
        KnowledgeItemPo item = knowledgeSaveSupport.requireActiveItem(dto.getKnowledgeId(), organId);
        knowledgeSaveSupport.assertCompatibleSource(item.getKnowledgeType(), dto.getSourceType());
        dto.setOrganId(organId);
        dto.setCreateUserId(knowledgeAccessService.requireUserId());
        dto.setRevisionStatus(KnowledgeRevisionStatus.DRAFT.name());
        dto.setChunkCount(0);
        dto.setIndexStatus(PENDING);
        if (KnowledgeSourceType.FILE.name().equals(dto.getSourceType())) {
            dto.setFaqQuestion(null);
            dto.setFaqAnswer(null);
        }
    }

    private KnowledgeRevisionPo buildDraftUpdate(KnowledgeRevisionDto dto, KnowledgeRevisionPo existing) {
        KnowledgeRevisionPo update = new KnowledgeRevisionPo();
        update.setId(existing.getId());
        update.setVersion(dto.getVersion());
        if (Objects.nonNull(dto.getRevisionNo())) {
            update.setRevisionNo(dto.getRevisionNo());
        }
        if (KnowledgeSourceType.FAQ.name().equals(existing.getSourceType())) {
            if (Objects.nonNull(dto.getFaqQuestion())) {
                update.setFaqQuestion(dto.getFaqQuestion());
            }
            if (Objects.nonNull(dto.getFaqAnswer())) {
                update.setFaqAnswer(dto.getFaqAnswer());
            }
        }
        update.setUpdateTime(Moments.now());
        return update;
    }

    private static void clearProtectedSystemFields(KnowledgeRevisionPo entity) {
        clearFileMetadata(entity);
        entity.setParserName(null);
        entity.setParserVersion(null);
        entity.setParserTaskId(null);
        entity.setParsedStoragePath(null);
        entity.setEmbeddingProfile(null);
        entity.setEmbeddingModel(null);
        entity.setIndexCompletedTime(null);
        entity.setReviewerUserId(null);
        entity.setReviewTime(null);
        entity.setReviewComment(null);
    }

    private static void clearFileMetadata(KnowledgeRevisionPo entity) {
        entity.setSourceFileName(null);
        entity.setSourceContentType(null);
        entity.setSourceStoragePath(null);
        entity.setSourceSha256(null);
        entity.setSourceFileSize(null);
    }
}
