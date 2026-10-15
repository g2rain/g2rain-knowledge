package com.g2rain.knowledge.service;

import com.g2rain.knowledge.client.storage.SourceMetadata;
import com.g2rain.knowledge.dto.KnowledgeCreateDto;
import com.g2rain.knowledge.dto.KnowledgeDraftCreateDto;
import com.g2rain.knowledge.dto.KnowledgeDraftEditDto;
import com.g2rain.knowledge.dto.KnowledgeFaqEditDto;

import java.io.InputStream;

/**
 * Domain operations for topics and mutable knowledge revisions.
 */
public interface KnowledgeDraftService {
    Long createKnowledge(KnowledgeCreateDto dto);

    Long createDraft(KnowledgeDraftCreateDto dto);

    void editDraft(KnowledgeDraftEditDto dto);

    void editFaq(KnowledgeFaqEditDto dto);

    /**
     * Stores a new source file then binds it to an editable FILE revision.
     * File IO runs outside the DB transaction; unreferenced uploads are cleaned up on failure.
     */
    void replaceFile(Long revisionId, Integer expectedVersion, InputStream content, SourceMetadata metadata);
}
