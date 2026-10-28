package com.g2rain.knowledge.client.milvus;

import java.util.List;
import java.util.stream.Collectors;

/** Builds Milvus expressions from server-validated tag codes only. */
final class MilvusFilterBuilder {
    private MilvusFilterBuilder() {
    }

    static String searchable(Long organId, boolean includePublic, List<String> allTagCodes, List<String> anyTagCodes) {
        StringBuilder filter = new StringBuilder("searchable == true && (organ_id == ").append(organId);
        if (includePublic) {
            filter.append(" || visibility == \"PUBLIC\"");
        }
        filter.append(')');
        if (!allTagCodes.isEmpty()) {
            filter.append(" && ARRAY_CONTAINS_ALL(tags, ").append(tags(allTagCodes)).append(')');
        }
        if (!anyTagCodes.isEmpty()) {
            filter.append(" && ARRAY_CONTAINS_ANY(tags, ").append(tags(anyTagCodes)).append(')');
        }
        return filter.toString();
    }

    private static String tags(List<String> values) {
        return values.stream().map(value -> "\"" + value + "\"").collect(Collectors.joining(",", "[", "]"));
    }
}
