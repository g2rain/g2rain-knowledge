package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.client.milvus.MilvusSearchCandidate;
import com.g2rain.knowledge.client.storage.KnowledgeStorageClient;
import com.g2rain.knowledge.dao.KnowledgeChunkDao;
import com.g2rain.knowledge.dao.KnowledgeItemDao;
import com.g2rain.knowledge.dao.KnowledgeReleaseDao;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.po.KnowledgeChunkPo;
import com.g2rain.knowledge.dao.po.KnowledgeItemPo;
import com.g2rain.knowledge.dao.po.KnowledgeReleasePo;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.domain.ContentHashes;
import com.g2rain.knowledge.domain.TitlePaths;
import com.g2rain.knowledge.dto.KnowledgeChunkSelectDto;
import com.g2rain.knowledge.dto.KnowledgeReleaseSelectDto;
import com.g2rain.knowledge.enums.KnowledgeEnablementStatus;
import com.g2rain.knowledge.enums.KnowledgeRevisionStatus;
import com.g2rain.knowledge.enums.KnowledgeVisibility;
import com.g2rain.knowledge.service.KnowledgePublicEvidenceVerifier;
import com.g2rain.knowledge.vo.KnowledgeEvidenceVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class KnowledgePublicEvidenceVerifierImpl implements KnowledgePublicEvidenceVerifier {
    @Resource private KnowledgeChunkDao knowledgeChunkDao;
    @Resource private KnowledgeRevisionDao knowledgeRevisionDao;
    @Resource private KnowledgeReleaseDao knowledgeReleaseDao;
    @Resource private KnowledgeItemDao knowledgeItemDao;
    @Resource private KnowledgeStorageClient knowledgeStorageClient;

    @Override
    public Optional<KnowledgeEvidenceVo> verifyCandidate(Long callerOrganId, boolean includePublic,
                                                         MilvusSearchCandidate candidate) {
        KnowledgeChunkPo chunk = knowledgeChunkDao.selectByIdWithoutIsolation(candidate.chunkId());
        if (chunk == null || !"INDEXED".equals(chunk.getChunkStatus())) {
            return Optional.empty();
        }
        return verifyRevisionChunk(callerOrganId, includePublic, false, chunk, candidate.score());
    }

    @Override
    public Optional<KnowledgeEvidenceVo> verifyKnowledge(Long callerOrganId, boolean publicCatalog, Long knowledgeId) {
        KnowledgeItemPo item = knowledgeItemDao.selectByIdWithoutIsolation(knowledgeId);
        if (item == null || !KnowledgeEnablementStatus.ACTIVE.name().equals(item.getStatus())) {
            return Optional.empty();
        }
        if (!KnowledgeVisibility.isPublic(item.getVisibility())) {
            return Optional.empty();
        }
        if (!visible(callerOrganId, true, true, item)) {
            return Optional.empty();
        }
        KnowledgeReleaseSelectDto releaseQuery = new KnowledgeReleaseSelectDto();
        releaseQuery.setOrganId(item.getOrganId());
        releaseQuery.setKnowledgeId(item.getId());
        releaseQuery.setReleaseStatus("PUBLISHED");
        List<KnowledgeReleasePo> releases = knowledgeReleaseDao.selectListWithoutIsolation(releaseQuery);
        if (releases.size() != 1 || releases.getFirst().getActiveRevisionId() == null) {
            return Optional.empty();
        }
        KnowledgeRevisionPo revision = knowledgeRevisionDao.selectByIdWithoutIsolation(
            releases.getFirst().getActiveRevisionId());
        if (revision == null
            || !KnowledgeRevisionStatus.PUBLISHED.name().equals(revision.getRevisionStatus())
            || !"INDEXED".equals(revision.getIndexStatus())) {
            return Optional.empty();
        }
        KnowledgeChunkSelectDto chunkQuery = new KnowledgeChunkSelectDto();
        chunkQuery.setRevisionId(revision.getId());
        List<KnowledgeChunkPo> chunks = knowledgeChunkDao.selectListWithoutIsolation(chunkQuery).stream()
            .filter(c -> "INDEXED".equals(c.getChunkStatus()))
            .sorted((a, b) -> {
                int left = a.getChunkNo() == null ? Integer.MAX_VALUE : a.getChunkNo();
                int right = b.getChunkNo() == null ? Integer.MAX_VALUE : b.getChunkNo();
                return Integer.compare(left, right);
            })
            .toList();
        if (chunks.isEmpty()) {
            return Optional.empty();
        }
        return buildCatalogBody(item, revision, chunks);
    }

    private Optional<KnowledgeEvidenceVo> buildCatalogBody(KnowledgeItemPo item, KnowledgeRevisionPo revision,
                                                           List<KnowledgeChunkPo> chunks) {
        StringBuilder body = new StringBuilder();
        for (KnowledgeChunkPo chunk : chunks) {
            String part;
            try {
                part = knowledgeStorageClient.readParsedChunk(revision.getParsedStoragePath(), chunk.getSourceLocator());
            } catch (BusinessException exception) {
                return Optional.empty();
            }
            if (!ContentHashes.matches(part, chunk.getContentSha256())) {
                return Optional.empty();
            }
            if (!body.isEmpty()) {
                body.append('\n');
            }
            body.append(part);
        }
        KnowledgeChunkPo first = chunks.getFirst();
        KnowledgeEvidenceVo evidence = new KnowledgeEvidenceVo();
        evidence.setKnowledgeId(item.getId());
        evidence.setRevisionId(revision.getId());
        evidence.setChunkId(first.getId());
        evidence.setOrganId(item.getOrganId());
        evidence.setCategoryId(item.getCategoryId());
        evidence.setVisibility(KnowledgeVisibility.parse(item.getVisibility()).name());
        evidence.setContent(body.toString());
        evidence.setTitle(item.getKnowledgeName());
        evidence.setSection(TitlePaths.toDisplay(first.getTitlePath()));
        evidence.setPage(first.getPageStart());
        evidence.setRevisionNo(revision.getRevisionNo());
        evidence.setCitation(item.getKnowledgeName() + " " + revision.getRevisionNo());
        return Optional.of(evidence);
    }

    private Optional<KnowledgeEvidenceVo> verifyRevisionChunk(Long callerOrganId, boolean includePublic,
                                                              boolean publicCatalog, KnowledgeChunkPo chunk,
                                                              Float score) {
        KnowledgeRevisionPo revision = knowledgeRevisionDao.selectByIdWithoutIsolation(chunk.getRevisionId());
        if (revision == null
            || !KnowledgeRevisionStatus.PUBLISHED.name().equals(revision.getRevisionStatus())
            || !"INDEXED".equals(revision.getIndexStatus())
            || !Objects.equals(revision.getOrganId(), chunk.getOrganId())) {
            return Optional.empty();
        }
        KnowledgeItemPo item = knowledgeItemDao.selectByIdWithoutIsolation(revision.getKnowledgeId());
        if (item == null
            || !KnowledgeEnablementStatus.ACTIVE.name().equals(item.getStatus())
            || !Objects.equals(item.getOrganId(), revision.getOrganId())) {
            return Optional.empty();
        }
        if (!visible(callerOrganId, includePublic, publicCatalog, item)) {
            return Optional.empty();
        }
        KnowledgeReleaseSelectDto releaseQuery = new KnowledgeReleaseSelectDto();
        releaseQuery.setOrganId(item.getOrganId());
        releaseQuery.setKnowledgeId(item.getId());
        releaseQuery.setReleaseStatus("PUBLISHED");
        List<KnowledgeReleasePo> releases = knowledgeReleaseDao.selectListWithoutIsolation(releaseQuery);
        if (releases.size() != 1 || !Objects.equals(releases.getFirst().getActiveRevisionId(), revision.getId())) {
            return Optional.empty();
        }
        return buildEvidence(item, revision, chunk, score);
    }

    private boolean visible(Long callerOrganId, boolean includePublic, boolean publicCatalog, KnowledgeItemPo item) {
        if (Objects.equals(item.getOrganId(), callerOrganId)) {
            return true;
        }
        if (!(includePublic || publicCatalog)) {
            return false;
        }
        return KnowledgeVisibility.isPublic(item.getVisibility());
    }

    private Optional<KnowledgeEvidenceVo> buildEvidence(KnowledgeItemPo item, KnowledgeRevisionPo revision,
                                                        KnowledgeChunkPo chunk, Float score) {
        String content;
        try {
            content = knowledgeStorageClient.readParsedChunk(revision.getParsedStoragePath(), chunk.getSourceLocator());
        } catch (BusinessException exception) {
            return Optional.empty();
        }
        if (!ContentHashes.matches(content, chunk.getContentSha256())) {
            return Optional.empty();
        }
        KnowledgeEvidenceVo evidence = new KnowledgeEvidenceVo();
        evidence.setKnowledgeId(item.getId());
        evidence.setRevisionId(revision.getId());
        evidence.setChunkId(chunk.getId());
        evidence.setOrganId(item.getOrganId());
        evidence.setCategoryId(item.getCategoryId());
        evidence.setVisibility(KnowledgeVisibility.parse(item.getVisibility()).name());
        evidence.setContent(content);
        evidence.setTitle(item.getKnowledgeName());
        String section = TitlePaths.toDisplay(chunk.getTitlePath());
        evidence.setSection(section);
        evidence.setPage(chunk.getPageStart());
        evidence.setRevisionNo(revision.getRevisionNo());
        String location = chunk.getPageStart() == null ? section : "第" + chunk.getPageStart() + " 页";
        evidence.setCitation(item.getKnowledgeName() + " " + revision.getRevisionNo() + "，"
            + (location == null || location.isBlank() ? "内容片段" : location));
        evidence.setScore(score);
        return Optional.of(evidence);
    }
}
