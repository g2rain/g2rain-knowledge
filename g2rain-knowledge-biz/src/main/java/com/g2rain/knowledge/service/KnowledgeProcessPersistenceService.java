package com.g2rain.knowledge.service;

import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.task.ParsedChunk;
import java.util.List;

public interface KnowledgeProcessPersistenceService {
    void saveParsed(KnowledgeRevisionPo revision, String parsedPath, List<ParsedChunk> chunks);

    void complete(Long revisionId);

    void fail(Long revisionId);
}
