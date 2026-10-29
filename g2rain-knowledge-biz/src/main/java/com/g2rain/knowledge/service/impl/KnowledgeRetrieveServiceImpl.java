package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.ErrorCode;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.knowledge.client.Deadline;
import com.g2rain.knowledge.client.KnowledgeDependencyException;
import com.g2rain.knowledge.client.milvus.MilvusKnowledgeClient;
import com.g2rain.knowledge.client.milvus.MilvusProperties;
import com.g2rain.knowledge.client.milvus.MilvusSearchCandidate;
import com.g2rain.knowledge.client.model.EmbeddingClient;
import com.g2rain.knowledge.client.model.KnowledgeRerankClient;
import com.g2rain.knowledge.client.model.KnowledgeRerankProperties;
import com.g2rain.knowledge.client.model.RerankScore;
import com.g2rain.knowledge.client.retrieve.KnowledgeRetrieveProperties;
import com.g2rain.knowledge.dao.KnowledgeTagDao;
import com.g2rain.knowledge.dto.KnowledgeRetrieveDto;
import com.g2rain.knowledge.dto.KnowledgeTagSelectDto;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgePublicEvidenceVerifier;
import com.g2rain.knowledge.service.KnowledgeRetrieveService;
import com.g2rain.knowledge.vo.KnowledgeEvidenceVo;
import com.g2rain.knowledge.vo.KnowledgeRetrieveVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Retrieves vector candidates, then makes MySQL the final authority for evidence. */
@Service
public class KnowledgeRetrieveServiceImpl implements KnowledgeRetrieveService {
    private static final Pattern TAG_CODE = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final Set<String> DEPENDENCY_CODES = Set.of(
        KnowledgeErrorCode.EMBEDDING_NOT_CONFIGURED.code(),
        KnowledgeErrorCode.EMBEDDING_OPERATION_FAILED.code(),
        KnowledgeErrorCode.EMBEDDING_INVALID_RESPONSE.code(),
        KnowledgeErrorCode.MILVUS_OPERATION_FAILED.code(),
        KnowledgeErrorCode.MILVUS_INVALID_RESPONSE.code(),
        KnowledgeErrorCode.RERANK_NOT_CONFIGURED.code(),
        KnowledgeErrorCode.RERANK_OPERATION_FAILED.code(),
        KnowledgeErrorCode.RERANK_INVALID_RESPONSE.code(),
        KnowledgeErrorCode.STORAGE_NOT_CONFIGURED.code(),
        KnowledgeErrorCode.STORAGE_OPERATION_FAILED.code(),
        KnowledgeErrorCode.DEPENDENCY_UNAVAILABLE.code(),
        KnowledgeErrorCode.RETRIEVE_DEADLINE_EXCEEDED.code()
    );

    @Resource private KnowledgeAccessService knowledgeAccessService;
    @Resource private KnowledgeTagDao knowledgeTagDao;
    @Resource private KnowledgePublicEvidenceVerifier knowledgePublicEvidenceVerifier;
    @Resource private EmbeddingClient embeddingClient;
    @Resource private KnowledgeRerankClient knowledgeRerankClient;
    @Resource private KnowledgeRerankProperties rerankProperties;
    @Resource private MilvusKnowledgeClient milvusKnowledgeClient;
    @Resource private MilvusProperties milvusProperties;
    @Resource private KnowledgeRetrieveProperties retrieveProperties;

    @Override
    public KnowledgeRetrieveVo retrieve(KnowledgeRetrieveDto dto) {
        Deadline deadline = Deadline.afterMillis(retrieveProperties.getTotalTimeoutMs());
        Long organId = knowledgeAccessService.requireOrganId();
        boolean includePublic = Boolean.TRUE.equals(dto.getIncludePublic());
        String query = dto.getQuery().trim();
        int topK = topK(dto.getTopK());
        List<String> allTags = tags(organId, dto.getAllTagCodes());
        List<String> anyTags = tags(organId, dto.getAnyTagCodes());

        deadline.check();
        float[] vector = dependency("embedding", () -> embeddingClient.embed(List.of(query)).getFirst());
        deadline.check();
        List<MilvusSearchCandidate> candidates = dependency("milvus", () -> milvusKnowledgeClient.search(
            milvusProperties.getCollectionName(), organId, includePublic, allTags, anyTags, vector,
            Math.min(retrieveProperties.getMaxTopK() * 3, topK * 3)));

        List<KnowledgeEvidenceVo> verified = new ArrayList<>();
        for (MilvusSearchCandidate candidate : candidates) {
            deadline.check();
            knowledgePublicEvidenceVerifier.verifyCandidate(organId, includePublic, candidate)
                .ifPresent(verified::add);
        }
        deadline.check();
        List<KnowledgeEvidenceVo> sources = rerank(query, verified, topK);
        KnowledgeRetrieveVo result = new KnowledgeRetrieveVo();
        result.setAnswerable(!sources.isEmpty());
        result.setTraceId(PrincipalContextHolder.getTraceId());
        result.setReasonCode(sources.isEmpty() ? "NO_EVIDENCE" : null);
        result.setSources(List.copyOf(sources));
        return result;
    }

    private List<KnowledgeEvidenceVo> rerank(String query, List<KnowledgeEvidenceVo> evidence, int topK) {
        if (evidence.isEmpty()) {
            return List.of();
        }
        if (!rerankProperties.isEnabled()) {
            return evidence.stream().limit(topK).toList();
        }
        List<RerankScore> scores = dependency("rerank",
            () -> knowledgeRerankClient.rerank(query, evidence.stream().map(KnowledgeEvidenceVo::getContent).toList(), topK));
        return scores.stream().map(score -> {
            KnowledgeEvidenceVo item = evidence.get(score.index());
            item.setScore(score.score());
            return item;
        }).limit(topK).toList();
    }

    private int topK(Integer requested) {
        int value = requested == null ? retrieveProperties.getDefaultTopK() : requested;
        if (value < 1 || value > retrieveProperties.getMaxTopK()) {
            throw new BusinessException(KnowledgeErrorCode.INVALID_RETRIEVE_TOP_K);
        }
        return value;
    }

    private List<String> tags(Long organId, List<String> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        List<String> tags = new ArrayList<>(new LinkedHashSet<>(input.stream()
            .map(value -> value == null ? "" : value.trim()).toList()));
        for (String tag : tags) {
            if (!TAG_CODE.matcher(tag).matches()) {
                throw new BusinessException(KnowledgeErrorCode.INVALID_TAG_CODE);
            }
            KnowledgeTagSelectDto query = new KnowledgeTagSelectDto();
            query.setOrganId(organId);
            query.setTagCode(tag);
            if (knowledgeTagDao.selectList(query).isEmpty()) {
                throw new BusinessException(KnowledgeErrorCode.TAG_NOT_AVAILABLE);
            }
        }
        return List.copyOf(tags);
    }

    private <T> T dependency(String name, DependencyCall<T> call) {
        try {
            return call.execute();
        } catch (KnowledgeDependencyException exception) {
            throw exception;
        } catch (BusinessException exception) {
            throw asDependency(name, exception);
        } catch (RuntimeException exception) {
            throw new KnowledgeDependencyException(name, KnowledgeErrorCode.DEPENDENCY_UNAVAILABLE, exception);
        }
    }

    private KnowledgeDependencyException asDependency(String name, BusinessException exception) {
        if (exception instanceof KnowledgeDependencyException dependencyException) {
            return dependencyException;
        }
        ErrorCode code = isDependency(exception)
            ? resolveErrorCode(exception.getErrorCode())
            : KnowledgeErrorCode.DEPENDENCY_UNAVAILABLE;
        return new KnowledgeDependencyException(name, code, exception);
    }

    private boolean isDependency(BusinessException exception) {
        return exception instanceof KnowledgeDependencyException
            || DEPENDENCY_CODES.contains(exception.getErrorCode());
    }

    private ErrorCode resolveErrorCode(String code) {
        for (KnowledgeErrorCode value : KnowledgeErrorCode.values()) {
            if (value.code().equals(code)) {
                return value;
            }
        }
        return KnowledgeErrorCode.DEPENDENCY_UNAVAILABLE;
    }

    @FunctionalInterface
    private interface DependencyCall<T> {
        T execute();
    }
}
