package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.id.IdGenerator;
import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.utils.Asserts;
import com.g2rain.common.utils.Moments;
import com.g2rain.common.validation.Validations;
import com.g2rain.knowledge.converter.KnowledgeReleaseConverter;
import com.g2rain.knowledge.client.milvus.MilvusKnowledgeClient;
import com.g2rain.knowledge.client.milvus.MilvusProperties;
import com.g2rain.knowledge.dao.KnowledgeItemDao;
import com.g2rain.knowledge.dao.KnowledgeReleaseDao;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.po.KnowledgeItemPo;
import com.g2rain.knowledge.dao.po.KnowledgeReleasePo;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.dto.KnowledgeReleaseDto;
import com.g2rain.knowledge.dto.KnowledgeReleasePublishDto;
import com.g2rain.knowledge.dto.KnowledgeReleaseRollbackDto;
import com.g2rain.knowledge.dto.KnowledgeReleaseSelectDto;
import com.g2rain.knowledge.domain.KnowledgePublicWriteGuard;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.enums.KnowledgeRevisionStatus;
import com.g2rain.knowledge.enums.KnowledgeVisibility;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeReleaseService;
import com.g2rain.knowledge.vo.KnowledgeReleaseVo;
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
 * 知识发布选择表服务实现类
 * 表名: knowledge_release
 *
 * @author G2rain Generator
 */
@Slf4j
@Service(value = "knowledgeReleaseServiceImpl")
public class KnowledgeReleaseServiceImpl implements KnowledgeReleaseService {

    @Resource(name = "knowledgeReleaseDao")
    private KnowledgeReleaseDao knowledgeReleaseDao;

    @Resource
    private KnowledgeRevisionDao knowledgeRevisionDao;
    @Resource
    private KnowledgeItemDao knowledgeItemDao;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;
    @Resource
    private MilvusKnowledgeClient milvusKnowledgeClient;
    @Resource
    private MilvusProperties milvusProperties;
    @Resource
    private KnowledgePublicWriteGuard knowledgePublicWriteGuard;

    private IdGenerator idGenerator;

    @Qualifier("idGenerator")
    @Autowired(required = false)
    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public List<KnowledgeReleaseVo> selectList(KnowledgeReleaseSelectDto selectDto) {
        return knowledgeReleaseDao.selectList(selectDto)
            .stream()
            .map(KnowledgeReleaseConverter.INSTANCE::po2vo)
            .toList();
    }

    @Override
    public PageData<KnowledgeReleaseVo> selectPage(PageSelectListDto<KnowledgeReleaseSelectDto> selectDto) {
        Page<KnowledgeReleasePo> page = PageContext.of(selectDto.getPageNum(), selectDto.getPageSize(), () -> {
            knowledgeReleaseDao.selectList(selectDto.getQuery());
        });
        List<KnowledgeReleaseVo> result = page.getResult()
            .stream()
            .map(KnowledgeReleaseConverter.INSTANCE::po2vo)
            .toList();
        return PageData.of(page.getPageNum(), page.getPageSize(), page.getTotal(), result);
    }

    @Override
    public Long save(KnowledgeReleaseDto dto) {
        Long organId = knowledgeAccessService.requireOrganId();
        Long id = dto.getId();
        boolean creating = Objects.isNull(id) || id == 0;
        if (!creating) {
            throw new BusinessException(KnowledgeErrorCode.RELEASE_SAVE_UPDATE_FORBIDDEN);
        }

        KnowledgeItemPo item = requireItemForRelease(dto.getKnowledgeId(), organId);

        dto.setOrganId(organId);
        dto.setReleaseStatus("UNPUBLISHED");
        dto.setActiveRevisionId(null);
        dto.setPublishedUserId(null);
        dto.setPublishedTime(null);
        dto.setReleaseReason(null);
        Validations.validateSave(dto);

        KnowledgeReleasePo entity = KnowledgeReleaseConverter.INSTANCE.dto2po(dto);
        entity.setId(idGenerator.generateId());
        entity.setOrganId(organId);
        entity.setKnowledgeId(item.getId());
        entity.setReleaseStatus("UNPUBLISHED");
        entity.setActiveRevisionId(null);
        entity.setPublishedUserId(null);
        entity.setPublishedTime(null);
        entity.setReleaseReason(null);
        LocalDateTime now = Moments.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        Asserts.greaterThan(knowledgeReleaseDao.insert(entity), 0, SystemErrorCode.CREATE_DATA_ERROR);
        return entity.getId();
    }

    private KnowledgeItemPo requireItemForRelease(Long knowledgeId, Long organId) {
        KnowledgeItemPo item = knowledgeItemDao.selectById(knowledgeId);
        if (item == null) {
            throw new BusinessException(SystemErrorCode.DATA_NOT_EXISTS, knowledgeId);
        }
        if (!Objects.equals(item.getOrganId(), organId)) {
            throw new BusinessException(SystemErrorCode.UNAUTHORIZED, "跨机构知识主题");
        }
        return item;
    }

    @Override
    @Transactional
    public Long publish(KnowledgeReleasePublishDto dto) {
        return switchActiveRevision(dto, false);
    }

    @Override
    @Transactional
    public Long rollback(KnowledgeReleaseRollbackDto dto) {
        return switchActiveRevision(dto, true);
    }

    private Long switchActiveRevision(KnowledgeReleasePublishDto dto, boolean restoringDeprecated) {
        KnowledgeReleasePo release = requireRelease(dto);
        KnowledgeRevisionPo target = requireTarget(dto, release, restoringDeprecated);
        KnowledgeItemPo theme = knowledgeItemDao.selectById(release.getKnowledgeId());
        if (theme != null) {
            knowledgePublicWriteGuard.requireIfPublic(theme.getVisibility());
        }
        Long previousRevisionId = release.getActiveRevisionId();

        // One Milvus round-trip: searchable=true + theme visibility (avoids double Flush under rate=0.1).
        String visibility = theme == null
            ? "PRIVATE"
            : KnowledgeVisibility.parse(theme.getVisibility()).name();
        milvusKnowledgeClient.enablePublishedRevision(
            milvusProperties.getCollectionName(), target.getOrganId(), target.getId(), visibility);

        LocalDateTime now = Moments.now();
        KnowledgeReleasePo releaseUpdate = new KnowledgeReleasePo();
        releaseUpdate.setId(release.getId());
        releaseUpdate.setVersion(release.getVersion());
        releaseUpdate.setActiveRevisionId(target.getId());
        releaseUpdate.setReleaseStatus("PUBLISHED");
        releaseUpdate.setPublishedUserId(knowledgeAccessService.currentUserId());
        releaseUpdate.setPublishedTime(now);
        releaseUpdate.setReleaseReason(dto.getReason());
        releaseUpdate.setUpdateTime(now);
        if (knowledgeReleaseDao.updateByVersion(releaseUpdate) != 1) {
            throw new BusinessException(KnowledgeErrorCode.RELEASE_VERSION_CONFLICT);
        }

        updateRevisionStatus(target, target.getVersion(), KnowledgeRevisionStatus.PUBLISHED.name(), now);
        if (previousRevisionId != null && !previousRevisionId.equals(target.getId())) {
            KnowledgeRevisionPo previous = knowledgeRevisionDao.selectById(previousRevisionId);
            if (previous != null) {
                knowledgeAccessService.assertSameOrgan(previous.getOrganId());
                updateRevisionStatus(previous, previous.getVersion(), KnowledgeRevisionStatus.DEPRECATED.name(), now);
                // Best-effort: MySQL already switched; do not fail publish if old Milvus projection lags.
                try {
                    milvusKnowledgeClient.setSearchable(
                        milvusProperties.getCollectionName(), target.getOrganId(), previousRevisionId, false);
                } catch (RuntimeException exception) {
                    log.warn("disable previous milvus revision failed after publish: organId={}, previousRevisionId={}, reason={}",
                        target.getOrganId(), previousRevisionId, exception.getMessage());
                }
            }
        }
        return release.getId();
    }

    @Override
    public int delete(Long id) {
        KnowledgeReleasePo existing = knowledgeReleaseDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        knowledgeAccessService.assertSameOrgan(existing.getOrganId());
        if (!"UNPUBLISHED".equals(existing.getReleaseStatus()) || Objects.nonNull(existing.getActiveRevisionId())) {
            throw new BusinessException(KnowledgeErrorCode.RELEASE_DELETE_FORBIDDEN);
        }
        return knowledgeReleaseDao.delete(id);
    }

    private KnowledgeReleasePo requireRelease(KnowledgeReleasePublishDto dto) {
        KnowledgeReleasePo release = knowledgeReleaseDao.selectById(dto.getReleaseId());
        if (release == null) throw new BusinessException(SystemErrorCode.DATA_NOT_EXISTS, dto.getReleaseId());
        knowledgeAccessService.assertSameOrgan(release.getOrganId());
        return release;
    }

    private KnowledgeRevisionPo requireTarget(KnowledgeReleasePublishDto dto, KnowledgeReleasePo release, boolean restoringDeprecated) {
        KnowledgeRevisionPo target = knowledgeRevisionDao.selectById(dto.getRevisionId());
        if (target == null) throw new BusinessException(SystemErrorCode.DATA_NOT_EXISTS, dto.getRevisionId());
        knowledgeAccessService.assertSameOrgan(target.getOrganId());
        if (!Objects.equals(target.getKnowledgeId(), release.getKnowledgeId())) {
            throw new BusinessException(KnowledgeErrorCode.RELEASE_TARGET_MISMATCH);
        }
        boolean permitted = restoringDeprecated
            ? KnowledgeRevisionStatus.DEPRECATED.name().equals(target.getRevisionStatus())
            : KnowledgeRevisionStatus.READY_TO_PUBLISH.name().equals(target.getRevisionStatus())
                || KnowledgeRevisionStatus.PUBLISHED.name().equals(target.getRevisionStatus());
        if (!permitted) {
            throw new BusinessException(KnowledgeErrorCode.RELEASE_NOT_PUBLISHABLE);
        }
        return target;
    }

    private void updateRevisionStatus(KnowledgeRevisionPo revision, Integer expectedVersion, String status, LocalDateTime now) {
        KnowledgeRevisionPo update = new KnowledgeRevisionPo();
        update.setId(revision.getId());
        update.setVersion(expectedVersion);
        update.setRevisionStatus(status);
        update.setUpdateTime(now);
        if (knowledgeRevisionDao.updateByVersion(update) != 1) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_VERSION_CONFLICT);
        }
    }
}
