package com.g2rain.knowledge.service;

import java.util.List;

/**
 * Verifies tag snapshots selected for new or editable drafts.
 */
public interface KnowledgeTagGovernanceService {
    List<String> validateDraftTags(List<String> codes);
}
