package com.g2rain.knowledge.enums;

/** Theme sharing scope (RQ-008). Authoritative on knowledge_item; projected to Milvus. */
public enum KnowledgeVisibility {
    PRIVATE,
    PUBLIC;

    public static KnowledgeVisibility parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return PRIVATE;
        }
        return KnowledgeVisibility.valueOf(raw.trim().toUpperCase());
    }

    public static boolean isPublic(String raw) {
        return PUBLIC.name().equals(parse(raw).name());
    }
}
