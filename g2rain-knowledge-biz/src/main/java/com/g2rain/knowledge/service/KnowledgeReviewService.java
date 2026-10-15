package com.g2rain.knowledge.service;

import com.g2rain.knowledge.dto.KnowledgeReviewDto;

/**
 * Applies the explicit human review transition for parsed knowledge.
 */
public interface KnowledgeReviewService {
    void submit(KnowledgeReviewDto dto);

    Long approve(KnowledgeReviewDto dto);

    void reject(KnowledgeReviewDto dto);
}
