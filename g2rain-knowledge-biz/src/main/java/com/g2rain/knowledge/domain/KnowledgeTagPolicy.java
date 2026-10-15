package com.g2rain.knowledge.domain;

import com.g2rain.common.exception.BusinessException;
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
}
