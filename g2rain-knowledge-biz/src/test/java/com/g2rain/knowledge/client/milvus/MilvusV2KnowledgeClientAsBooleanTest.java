package com.g2rain.knowledge.client.milvus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MilvusV2KnowledgeClientAsBooleanTest {

    @Test
    void acceptsCommonMilvusSearchableShapes() {
        assertTrue(MilvusV2KnowledgeClient.asBoolean(Boolean.TRUE));
        assertTrue(MilvusV2KnowledgeClient.asBoolean(true));
        assertTrue(MilvusV2KnowledgeClient.asBoolean(1));
        assertTrue(MilvusV2KnowledgeClient.asBoolean(1L));
        assertTrue(MilvusV2KnowledgeClient.asBoolean("true"));
        assertTrue(MilvusV2KnowledgeClient.asBoolean("TRUE"));
        assertTrue(MilvusV2KnowledgeClient.asBoolean("1"));

        assertFalse(MilvusV2KnowledgeClient.asBoolean(null));
        assertFalse(MilvusV2KnowledgeClient.asBoolean(Boolean.FALSE));
        assertFalse(MilvusV2KnowledgeClient.asBoolean(false));
        assertFalse(MilvusV2KnowledgeClient.asBoolean(0));
        assertFalse(MilvusV2KnowledgeClient.asBoolean("false"));
        assertFalse(MilvusV2KnowledgeClient.asBoolean("0"));
        assertFalse(MilvusV2KnowledgeClient.asBoolean(""));
    }
}
