package com.g2rain.knowledge.task;

/** Internal deterministic chunk representation; text stays in the parsed artifact. */
public record ParsedChunk(int no, String locator, String sha256, int tokenCount) {
}
