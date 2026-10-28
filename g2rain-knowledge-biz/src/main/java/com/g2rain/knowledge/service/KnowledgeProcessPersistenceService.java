package com.g2rain.knowledge.service;

import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.domain.ChunkContent;

import java.util.List;

public interface KnowledgeProcessPersistenceService {
    void saveParsed(KnowledgeRevisionPo revision, String parsedPath, List<ChunkContent> chunks, String parserName);

    void complete(Long revisionId);

    void fail(Long revisionId);
}
