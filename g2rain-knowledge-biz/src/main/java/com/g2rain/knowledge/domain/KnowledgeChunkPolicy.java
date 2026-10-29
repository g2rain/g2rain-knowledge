package com.g2rain.knowledge.domain;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * FAQ chunking and shared chunk-manifest validation. FILE sources are chunked by Docling Serve.
 */
@Component
public class KnowledgeChunkPolicy {
    public static final String PARSER_FAQ = "FaqPolicy";
    public static final String PARSER_DOCLING_HYBRID = "DoclingHybrid";

    public List<ChunkContent> chunkFaq(String question, String answer, ChunkingProfile profile) {
        String q = Objects.requireNonNullElse(question, "").trim();
        String a = Objects.requireNonNullElse(answer, "").trim();
        if (q.isBlank() || a.isBlank()) {
            throw new BusinessException(KnowledgeErrorCode.CHUNK_MANIFEST_INVALID);
        }
        int maxTokens = profile == null ? 512 : profile.maxTokens();
        List<String> answerParts = splitByEstimatedTokens(a, maxTokens);
        List<ChunkContent> chunks = new ArrayList<>(answerParts.size());
        for (int i = 0; i < answerParts.size(); i++) {
            int no = i + 1;
            String raw = answerParts.size() == 1
                ? "# " + q + "\n\n" + answerParts.get(i)
                : answerParts.get(i);
            String embedding = "# " + q + "\n\n" + answerParts.get(i);
            chunks.add(build(no, "chunk:" + no, raw, embedding, estimateTokens(embedding), null, null,
                TitlePaths.toJson(List.of(q))));
        }
        return List.copyOf(chunks);
    }

    public void validateManifest(List<ChunkContent> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new BusinessException(KnowledgeErrorCode.CHUNK_MANIFEST_INVALID);
        }
        for (int i = 0; i < chunks.size(); i++) {
            ChunkContent chunk = chunks.get(i);
            if (chunk == null || chunk.chunkNo() != i + 1) {
                throw new BusinessException(KnowledgeErrorCode.CHUNK_MANIFEST_INVALID);
            }
            if (isBlank(chunk.sourceLocator()) || !chunk.sourceLocator().equals("chunk:" + chunk.chunkNo())) {
                throw new BusinessException(KnowledgeErrorCode.CHUNK_MANIFEST_INVALID);
            }
            if (isBlank(chunk.rawText()) || isBlank(chunk.embeddingText()) || isBlank(chunk.contentSha256())) {
                throw new BusinessException(KnowledgeErrorCode.CHUNK_MANIFEST_INVALID);
            }
            if (chunk.tokenCount() < 1) {
                throw new BusinessException(KnowledgeErrorCode.CHUNK_MANIFEST_INVALID);
            }
            if (!ContentHashes.matches(chunk.rawText(), chunk.contentSha256())) {
                throw new BusinessException(KnowledgeErrorCode.CHUNK_MANIFEST_INVALID);
            }
            Integer start = chunk.pageStart();
            Integer end = chunk.pageEnd();
            if (start != null && start < 1) {
                throw new BusinessException(KnowledgeErrorCode.CHUNK_MANIFEST_INVALID);
            }
            if (end != null && end < 1) {
                throw new BusinessException(KnowledgeErrorCode.CHUNK_MANIFEST_INVALID);
            }
            if (start != null && end != null && end < start) {
                throw new BusinessException(KnowledgeErrorCode.CHUNK_MANIFEST_INVALID);
            }
        }
    }

    public ChunkContent build(int chunkNo, String locator, String rawText, String embeddingText,
                              int tokenCount, Integer pageStart, Integer pageEnd, String titlePath) {
        String raw = Objects.requireNonNullElse(rawText, "");
        String embed = isBlank(embeddingText) ? raw : embeddingText;
        return new ChunkContent(
            chunkNo,
            locator,
            raw,
            embed,
            Math.max(1, tokenCount),
            pageStart,
            pageEnd,
            titlePath,
            ContentHashes.sha256Utf8(raw)
        );
    }

    /** CJK-friendly estimate: CJK ideographs count as 1 token; other runs ~4 chars/token. */
    public int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int tokens = 0;
        int latinRun = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (isCjk(cp)) {
                if (latinRun > 0) {
                    tokens += (latinRun + 3) / 4;
                    latinRun = 0;
                }
                tokens += 1;
            } else if (Character.isWhitespace(cp)) {
                if (latinRun > 0) {
                    tokens += (latinRun + 3) / 4;
                    latinRun = 0;
                }
            } else {
                latinRun += 1;
            }
        }
        if (latinRun > 0) {
            tokens += (latinRun + 3) / 4;
        }
        return Math.max(1, tokens);
    }

    private List<String> splitByEstimatedTokens(String text, int maxTokens) {
        if (estimateTokens(text) <= maxTokens) {
            return List.of(text);
        }
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            int len = Character.charCount(cp);
            String next = text.substring(i, i + len);
            if (current.length() > 0 && estimateTokens(current + next) > maxTokens) {
                parts.add(current.toString());
                current.setLength(0);
            }
            current.append(next);
            i += len;
        }
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        return parts.isEmpty() ? List.of(text) : parts;
    }

    private static boolean isCjk(int cp) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(cp);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || block == Character.UnicodeBlock.HIRAGANA
            || block == Character.UnicodeBlock.KATAKANA
            || block == Character.UnicodeBlock.HANGUL_SYLLABLES;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
