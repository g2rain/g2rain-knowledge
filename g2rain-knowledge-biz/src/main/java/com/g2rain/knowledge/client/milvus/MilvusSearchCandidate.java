package com.g2rain.knowledge.client.milvus;

/** Candidate returned from Milvus before MySQL release-state verification. */
public record MilvusSearchCandidate(Long chunkId, Float score) {
}
