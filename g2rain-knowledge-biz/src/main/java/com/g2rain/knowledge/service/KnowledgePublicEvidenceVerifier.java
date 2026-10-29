package com.g2rain.knowledge.service;

import com.g2rain.knowledge.client.milvus.MilvusSearchCandidate;
import com.g2rain.knowledge.vo.KnowledgeEvidenceVo;

import java.util.Optional;

/** MySQL authority check for retrieve/public read using WithoutIsolation reads (DEV-004). */
public interface KnowledgePublicEvidenceVerifier {
    Optional<KnowledgeEvidenceVo> verifyCandidate(Long callerOrganId, boolean includePublic, MilvusSearchCandidate candidate);

    Optional<KnowledgeEvidenceVo> verifyKnowledge(Long callerOrganId, boolean publicCatalog, Long knowledgeId);
}
