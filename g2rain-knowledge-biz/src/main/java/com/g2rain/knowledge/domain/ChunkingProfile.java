package com.g2rain.knowledge.domain;

/**
 * Controlled HybridChunker / FAQ chunking knobs. Profile version is for task snapshots.
 */
public record ChunkingProfile(int maxTokens, String tokenizer, boolean mergePeers, String profileVersion) {
    public static final String DEFAULT_TOKENIZER = "sentence-transformers/all-MiniLM-L6-v2";
    public static final String LOCAL_CJK_ESTIMATOR = "cjk-char-estimate-v1";
    public static final String PROFILE_VERSION = "hybrid-v1";

    public static ChunkingProfile defaults(int maxTokens) {
        return new ChunkingProfile(Math.max(1, maxTokens), DEFAULT_TOKENIZER, true, PROFILE_VERSION);
    }
}
