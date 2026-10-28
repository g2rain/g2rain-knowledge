package com.g2rain.knowledge.client.milvus;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MilvusFilterBuilderTest {

    @Test
    void withoutIncludePublicKeepsOrganOnly() {
        String filter = MilvusFilterBuilder.searchable(7L, false, List.of(), List.of());
        assertEquals("searchable == true && (organ_id == 7)", filter);
        assertFalse(filter.contains("PUBLIC"));
    }

    @Test
    void withIncludePublicAddsVisibilityOr() {
        String filter = MilvusFilterBuilder.searchable(7L, true, List.of("a"), List.of("b"));
        assertTrue(filter.contains("organ_id == 7 || visibility == \"PUBLIC\""));
        assertTrue(filter.contains("ARRAY_CONTAINS_ALL(tags, [\"a\"])"));
        assertTrue(filter.contains("ARRAY_CONTAINS_ANY(tags, [\"b\"])"));
    }
}
