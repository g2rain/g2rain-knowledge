package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.utils.Moments;
import com.g2rain.knowledge.client.storage.StoredSource;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.domain.KnowledgeRevisionStateMachine;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.enums.KnowledgeSourceType;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeDraftTxService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Applies the database reference to an already persisted source file.
 */
@Service
public class KnowledgeDraftTxServiceImpl implements KnowledgeDraftTxService {
    @Resource
    private KnowledgeRevisionDao knowledgeRevisionDao;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;
    @Resource
    private KnowledgeRevisionStateMachine revisionStateMachine;

    @Override
    @Transactional
    public void attachFile(Long revisionId, Integer expectedVersion, StoredSource source) {
        KnowledgeRevisionPo current = knowledgeRevisionDao.selectById(revisionId);
        if (Objects.isNull(current)) {
            throw new BusinessException(com.g2rain.common.exception.SystemErrorCode.DATA_NOT_EXISTS, revisionId);
        }
        knowledgeAccessService.assertSameOrgan(current.getOrganId());
        revisionStateMachine.requireEditable(current.getRevisionStatus());
        if (!Objects.equals(current.getVersion(), expectedVersion)) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_VERSION_CONFLICT);
        }
        if (!KnowledgeSourceType.FILE.name().equals(current.getSourceType())) {
            throw new BusinessException(KnowledgeErrorCode.SOURCE_TYPE_MISMATCH);
        }

        KnowledgeRevisionPo update = new KnowledgeRevisionPo();
        update.setId(revisionId);
        update.setVersion(expectedVersion);
        update.setSourceFileName(source.displayFileName());
        update.setSourceContentType(source.contentType());
        update.setSourceStoragePath(source.relativePath());
        update.setSourceSha256(source.sha256());
        update.setSourceFileSize(source.size());
        update.setUpdateTime(Moments.now());
        if (knowledgeRevisionDao.updateByVersion(update) != 1) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_VERSION_CONFLICT);
        }
    }
}
