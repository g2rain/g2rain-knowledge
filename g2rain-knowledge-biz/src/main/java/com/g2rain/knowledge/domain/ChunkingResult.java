package com.g2rain.knowledge.domain;

import java.util.List;

/**
 * Normalized Docling hybrid-chunk output: full markdown for audit plus chunk manifest.
 */
public record ChunkingResult(String documentMarkdown, List<ChunkContent> chunks, String parserName) {
}
