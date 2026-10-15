package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.knowledge.client.storage.KnowledgeStorageClient;
import com.g2rain.knowledge.client.storage.SourceMetadata;
import com.g2rain.knowledge.client.storage.StoredSource;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.enums.KnowledgeSourceType;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeDraftTxService;
import com.g2rain.knowledge.service.KnowledgeFileSourceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Objects;

/**
 * Stores the source before opening the short database transaction that refers to it.
 */
@Service
public class KnowledgeFileSourceServiceImpl implements KnowledgeFileSourceService {
    @Resource
    private KnowledgeRevisionDao knowledgeRevisionDao;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;
    @Resource
    private KnowledgeStorageClient knowledgeStorageClient;
    @Resource
    private KnowledgeDraftTxService knowledgeDraftTxService;

    @Override
    public void replaceFile(Long revisionId, Integer expectedVersion, InputStream content, SourceMetadata metadata) {
        KnowledgeRevisionPo revision = knowledgeRevisionDao.selectById(revisionId);
        if (Objects.isNull(revision)) {
            throw new BusinessException(SystemErrorCode.DATA_NOT_EXISTS, revisionId);
        }
        knowledgeAccessService.assertSameOrgan(revision.getOrganId());
        if (!KnowledgeSourceType.FILE.name().equals(revision.getSourceType())) {
            throw new BusinessException(KnowledgeErrorCode.SOURCE_TYPE_MISMATCH);
        }
        StoredSource stored = knowledgeStorageClient.storeSource(revision.getOrganId(), revision.getKnowledgeId(), revisionId,
            content, metadata);
        try {
            knowledgeDraftTxService.attachFile(revisionId, expectedVersion, stored);
        } catch (RuntimeException exception) {
            knowledgeStorageClient.deleteUnreferenced(stored.relativePath());
            throw exception;
        }
    }
}
