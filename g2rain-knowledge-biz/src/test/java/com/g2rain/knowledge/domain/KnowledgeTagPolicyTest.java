package com.g2rain.knowledge.domain;

import com.g2rain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KnowledgeTagPolicyTest {
    private final KnowledgeTagPolicy policy = new KnowledgeTagPolicy();

    @Test
    void preservesFirstOccurrenceAndBuildsSafeJsonSnapshot() {
        assertEquals("[\"after_sales\",\"x100\"]", policy.asJson(List.of("after_sales", "x100", "after_sales")));
    }

    @Test
    void rejectsCharactersThatWouldChangeJsonOrMilvusFilterMeaning() {
        assertThrows(BusinessException.class, () -> policy.normalize(List.of("after-sales")));
        assertThrows(BusinessException.class, () -> policy.normalize(List.of("x\"100")));
    }
}
