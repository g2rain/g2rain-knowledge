package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.id.IdGenerator;
import com.g2rain.common.utils.Moments;
import com.g2rain.knowledge.client.model.EmbeddingProperties;
import com.g2rain.knowledge.dao.KnowledgeChunkDao;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.po.KnowledgeChunkPo;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.domain.ChunkContent;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.enums.KnowledgeRevisionStatus;
import com.g2rain.knowledge.service.KnowledgeProcessPersistenceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class KnowledgeProcessPersistenceServiceImpl implements KnowledgeProcessPersistenceService {
    @Resource private KnowledgeRevisionDao knowledgeRevisionDao;
    @Resource private KnowledgeChunkDao knowledgeChunkDao;
    @Resource private IdGenerator idGenerator;
    @Resource private EmbeddingProperties embeddingProperties;

    @Override
    @Transactional
    public void saveParsed(KnowledgeRevisionPo revision, String parsedPath, List<ChunkContent> chunks, String parserName) {
        // Separate from index: retry clears previous chunk rows after Embedding/Milvus failure.
        knowledgeChunkDao.deletePhysicalByRevisionId(revision.getId());
        List<KnowledgeChunkPo> rows = chunks.stream().map(chunk -> {
            KnowledgeChunkPo row = new KnowledgeChunkPo();
            row.setId(idGenerator.generateId());
            row.setOrganId(revision.getOrganId());
            row.setRevisionId(revision.getId());
            row.setChunkNo(chunk.chunkNo());
            row.setSourceLocator(chunk.sourceLocator());
            row.setPageStart(chunk.pageStart());
            row.setPageEnd(chunk.pageEnd());
            row.setTokenCount(chunk.tokenCount());
            row.setContentSha256(chunk.contentSha256());
            row.setTitlePath(chunk.titlePath());
            row.setChunkStatus("PENDING");
            row.setCreateTime(Moments.now());
            row.setUpdateTime(Moments.now());
            return row;
        }).toList();
        if (!rows.isEmpty() && knowledgeChunkDao.insertMultiple(rows) != rows.size()) {
            throw new BusinessException(KnowledgeErrorCode.STORAGE_OPERATION_FAILED);
        }
        KnowledgeRevisionPo update = new KnowledgeRevisionPo();
        update.setId(revision.getId());
        update.setVersion(revision.getVersion());
        update.setParsedStoragePath(parsedPath);
        update.setParserName(Objects.requireNonNullElse(parserName, "DoclingHybrid"));
        update.setChunkCount(rows.size());
        update.setUpdateTime(Moments.now());
        if (knowledgeRevisionDao.updateByVersion(update) != 1) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_VERSION_CONFLICT);
        }
    }

    @Override
    @Transactional
    public void complete(Long revisionId) {
        KnowledgeRevisionPo current = knowledgeRevisionDao.selectById(revisionId);
        if (current == null) {
            return;
        }
        knowledgeChunkDao.markIndexedByRevisionId(revisionId, Moments.now());
        KnowledgeRevisionPo update = new KnowledgeRevisionPo();
        update.setId(revisionId);
        update.setVersion(current.getVersion());
        update.setRevisionStatus(KnowledgeRevisionStatus.READY_TO_PUBLISH.name());
        update.setIndexStatus("INDEXED");
        update.setEmbeddingProfile(embeddingProperties.getModelName() + "-" + embeddingProperties.getDimension());
        update.setEmbeddingModel(embeddingProperties.getModelName());
        update.setIndexCompletedTime(Moments.now());
        update.setUpdateTime(Moments.now());
        if (knowledgeRevisionDao.updateByVersion(update) != 1) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_VERSION_CONFLICT);
        }
    }

    @Override
    @Transactional
    public void fail(Long revisionId) {
        transition(revisionId, KnowledgeRevisionStatus.PROCESS_FAILED.name(), "FAILED");
    }

    private void transition(Long revisionId, String status, String indexStatus) {
        KnowledgeRevisionPo current = knowledgeRevisionDao.selectById(revisionId);
        if (current == null) {
            return;
        }
        KnowledgeRevisionPo update = new KnowledgeRevisionPo();
        update.setId(revisionId);
        update.setVersion(current.getVersion());
        update.setRevisionStatus(status);
        update.setIndexStatus(indexStatus);
        update.setUpdateTime(Moments.now());
        if (knowledgeRevisionDao.updateByVersion(update) != 1) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_VERSION_CONFLICT);
        }
    }
}
