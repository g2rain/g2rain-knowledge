package com.g2rain.knowledge.domain;

import com.g2rain.common.json.JsonCodec;
import com.g2rain.common.json.JsonCodecFactory;

import java.util.List;
import java.util.Objects;

/**
 * MySQL {@code knowledge_chunk.title_path} is JSON (array of heading segments).
 * This helper serializes for storage and formats for citation/UI display.
 */
public final class TitlePaths {
    private static final JsonCodec JSON = JsonCodecFactory.instance();

    private TitlePaths() {
    }

    /** Serializes cleaned heading segments as a JSON array string, or {@code null} if empty. */
    public static String toJson(List<String> segments) {
        if (segments == null || segments.isEmpty()) {
            return null;
        }
        List<String> cleaned = segments.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
        if (cleaned.isEmpty()) {
            return null;
        }
        return JSON.obj2str(cleaned);
    }

    /** Formats stored JSON array (or legacy plain text) as {@code A > B} for humans. */
    public static String toDisplay(String titlePath) {
        if (titlePath == null || titlePath.isBlank()) {
            return null;
        }
        String trimmed = titlePath.trim();
        if (trimmed.startsWith("[")) {
            try {
                List<String> segments = JSON.str2obj(trimmed, List.class, String.class);
                if (segments == null || segments.isEmpty()) {
                    return null;
                }
                List<String> cleaned = segments.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
                return cleaned.isEmpty() ? null : String.join(" > ", cleaned);
            } catch (RuntimeException ignored) {
                return trimmed;
            }
        }
        return trimmed;
    }
}
