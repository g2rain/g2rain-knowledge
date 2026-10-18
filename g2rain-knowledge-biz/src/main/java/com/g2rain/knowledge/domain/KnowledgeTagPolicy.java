package com.g2rain.knowledge.domain;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.json.JsonCodec;
import com.g2rain.common.json.JsonCodecFactory;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Normalizes immutable tag-code snapshots stored by a revision.
 */
@Component
public class KnowledgeTagPolicy {
    private static final Pattern TAG_CODE = Pattern.compile("[a-z0-9_]{1,64}");
    private static final JsonCodec JSON = JsonCodecFactory.instance();

    public List<String> normalize(List<String> codes) {
        if (Objects.isNull(codes) || codes.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String code : codes) {
            if (Objects.isNull(code) || !TAG_CODE.matcher(code).matches()) {
                throw new BusinessException(KnowledgeErrorCode.INVALID_TAG_CODE);
            }
            normalized.add(code);
        }
        return List.copyOf(normalized);
    }

    public String asJson(List<String> codes) {
        return normalize(codes).stream().map(code -> "\"" + code + "\"")
            .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    /**
     * Parses revision {@code tagsJson}. {@code null} or blank (including {@code ""}) becomes empty list.
     */
    public List<String> parseTagsJson(String tagsJson) {
        if (Objects.isNull(tagsJson) || tagsJson.isBlank()) {
            return List.of();
        }
        try {
            List<String> parsed = JSON.str2obj(tagsJson, List.class, String.class);
            return Objects.requireNonNullElse(parsed, List.of());
        } catch (RuntimeException exception) {
            throw new BusinessException(KnowledgeErrorCode.INVALID_TAGS_JSON);
        }
    }
}
