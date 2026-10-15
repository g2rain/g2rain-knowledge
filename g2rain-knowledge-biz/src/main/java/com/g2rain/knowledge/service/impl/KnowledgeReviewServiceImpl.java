package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.utils.Moments;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.dto.KnowledgeReviewDto;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.enums.KnowledgeRevisionStatus;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeReviewService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Reviews the original source and business metadata before technical processing.
 */
@Service
public class KnowledgeReviewServiceImpl implements KnowledgeReviewService {
    private static final String REVIEWING = KnowledgeRevisionStatus.REVIEWING.name();
    private static final String DRAFT = KnowledgeRevisionStatus.DRAFT.name();
    private static final String APPROVED = KnowledgeRevisionStatus.APPROVED.name();

    @Resource
    private KnowledgeRevisionDao knowledgeRevisionDao;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;

    @Override
    @Transactional
    public void submit(KnowledgeReviewDto dto) {
        KnowledgeRevisionPo revision = requireExisting(dto);
        if (!DRAFT.equals(revision.getRevisionStatus()) || !hasSource(revision)) {
            throw new BusinessException(KnowledgeErrorCode.SOURCE_NOT_READY);
        }
        transition(revision, dto, REVIEWING, null);
    }

    @Override
    @Transactional
    public Long approve(KnowledgeReviewDto dto) {
        KnowledgeRevisionPo revision = requireReviewable(dto);
        Long reviewerId = knowledgeAccessService.currentUserId();
        transition(revision, dto, APPROVED, reviewerId);
        return revision.getId();
    }

    @Override
    @Transactional
    public void reject(KnowledgeReviewDto dto) {
        if (Objects.isNull(dto.getComment()) || dto.getComment().isBlank()) {
            throw new BusinessException(KnowledgeErrorCode.REVIEW_COMMENT_REQUIRED);
        }
        KnowledgeRevisionPo revision = requireReviewable(dto);
        transition(revision, dto, DRAFT, knowledgeAccessService.currentUserId());
    }

    private KnowledgeRevisionPo requireExisting(KnowledgeReviewDto dto) {
        KnowledgeRevisionPo revision = knowledgeRevisionDao.selectById(dto.getRevisionId());
        if (Objects.isNull(revision)) {
            throw new BusinessException(SystemErrorCode.DATA_NOT_EXISTS, dto.getRevisionId());
        }
        knowledgeAccessService.assertSameOrgan(revision.getOrganId());
        if (!Objects.equals(revision.getVersion(), dto.getExpectedVersion())) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_VERSION_CONFLICT);
        }
        return revision;
    }

    private KnowledgeRevisionPo requireReviewable(KnowledgeReviewDto dto) {
        KnowledgeRevisionPo revision = requireExisting(dto);
        if (!REVIEWING.equals(revision.getRevisionStatus())) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_NOT_REVIEWABLE);
        }
        return revision;
    }

    private void transition(KnowledgeRevisionPo revision, KnowledgeReviewDto dto, String status, Long reviewerId) {
        var now = Moments.now();
        KnowledgeRevisionPo update = new KnowledgeRevisionPo();
        update.setId(revision.getId());
        update.setVersion(dto.getExpectedVersion());
        update.setRevisionStatus(status);
        update.setReviewerUserId(reviewerId);
        update.setReviewTime(now);
        update.setReviewComment(dto.getComment());
        update.setUpdateTime(now);
        if (knowledgeRevisionDao.updateByVersion(update) != 1) {
            throw new BusinessException(KnowledgeErrorCode.REVISION_VERSION_CONFLICT);
        }
    }

    private boolean hasSource(KnowledgeRevisionPo revision) {
        return "FILE".equals(revision.getSourceType())
            ? revision.getSourceStoragePath() != null && !revision.getSourceStoragePath().isBlank()
            : revision.getFaqQuestion() != null && !revision.getFaqQuestion().isBlank()
                && revision.getFaqAnswer() != null && !revision.getFaqAnswer().isBlank();
    }
}
