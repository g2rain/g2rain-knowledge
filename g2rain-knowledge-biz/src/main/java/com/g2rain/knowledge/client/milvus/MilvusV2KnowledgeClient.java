package com.g2rain.knowledge.client.milvus;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.DescribeCollectionReq;
import io.milvus.v2.service.utility.request.FlushReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.QueryResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Milvus V2 staged writer. Collection schema is owned by deployment migration. */
@Slf4j
@Component
public class MilvusV2KnowledgeClient implements MilvusKnowledgeClient {
    private static final List<String> FULL_ROW_FIELDS = List.of(
        "chunk_id", "organ_id", "knowledge_id", "revision_id", "searchable", "visibility", "tags",
        "source_type", "content", "title_path", "page_start", "page_end", "embedding_profile", "content_vector"
    );
    /** Enough for a single revision; avoids SDK default limit truncating confirm queries. */
    private static final long REVISION_QUERY_LIMIT = 16_384L;
    private static final int SEARCHABLE_CONFIRM_ATTEMPTS = 5;

    private final MilvusProperties properties;
    private volatile MilvusClientV2 client;

    public MilvusV2KnowledgeClient(MilvusProperties properties) { this.properties = properties; }

    @Override
    public void upsertStaged(String collectionName, List<MilvusIndexEntity> entities) {
        if (entities.isEmpty()) return;
        try {
            List<JsonObject> data = entities.stream().map(this::json).toList();
            client().upsert(UpsertReq.builder().collectionName(collectionName).data(data).build());
            flush(collectionName);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(KnowledgeErrorCode.MILVUS_OPERATION_FAILED, exception);
        }
    }

    @Override
    public long countRevision(String collectionName, Long organId, Long revisionId) {
        try {
            String filter = "organ_id == " + organId + " && revision_id == " + revisionId;
            return client().query(QueryReq.builder().collectionName(collectionName).filter(filter)
                .outputFields(List.of("chunk_id"))
                .consistencyLevel(ConsistencyLevel.STRONG)
                .limit(REVISION_QUERY_LIMIT)
                .build()).getQueryResults().size();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(KnowledgeErrorCode.MILVUS_OPERATION_FAILED, exception);
        }
    }

    @Override
    public void verifyStaged(String collectionName, List<MilvusIndexEntity> expected) {
        if (expected.isEmpty()) return;
        Long organId = expected.getFirst().organId(); Long revisionId = expected.getFirst().revisionId();
        try {
            String filter = "organ_id == " + organId + " && revision_id == " + revisionId;
            var rows = client().query(QueryReq.builder().collectionName(collectionName).filter(filter)
                .outputFields(List.of("chunk_id", "organ_id", "revision_id", "tags", "embedding_profile", "searchable"))
                .consistencyLevel(ConsistencyLevel.STRONG)
                .limit(REVISION_QUERY_LIMIT)
                .build()).getQueryResults();
            if (rows.size() != expected.size()) {
                log.warn("verifyStaged size mismatch: collection={}, organId={}, revisionId={}, expected={}, actual={}",
                    collectionName, organId, revisionId, expected.size(), rows.size());
                throw new BusinessException(KnowledgeErrorCode.MILVUS_INVALID_RESPONSE);
            }
            // SDK result maps are normalized here by field name; each expected entity must match all filter-critical fields.
            for (MilvusIndexEntity entity : expected) {
                boolean matched = rows.stream().anyMatch(row -> String.valueOf(row.getEntity().get("chunk_id")).equals(String.valueOf(entity.chunkId()))
                    && String.valueOf(row.getEntity().get("organ_id")).equals(String.valueOf(entity.organId()))
                    && String.valueOf(row.getEntity().get("revision_id")).equals(String.valueOf(entity.revisionId()))
                    && String.valueOf(row.getEntity().get("embedding_profile")).equals(entity.embeddingProfile())
                    && sameTags(row.getEntity().get("tags"), entity.tags())
                    && !asBoolean(row.getEntity().get("searchable")));
                if (!matched) {
                    log.warn("verifyStaged entity mismatch: collection={}, organId={}, revisionId={}, chunkId={}",
                        collectionName, organId, revisionId, entity.chunkId());
                    throw new BusinessException(KnowledgeErrorCode.MILVUS_INVALID_RESPONSE);
                }
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(KnowledgeErrorCode.MILVUS_OPERATION_FAILED, exception);
        }
    }

    private boolean sameTags(Object actual, List<String> expected) {
        if (actual instanceof List<?> values) {
            return values.stream().map(String::valueOf).sorted().toList().equals(expected.stream().sorted().toList());
        }
        String normalized = String.valueOf(actual).replace("[", "").replace("]", "").replace("\"", "");
        List<String> values = normalized.isBlank() ? List.of() : java.util.Arrays.stream(normalized.split(","))
            .map(String::trim).filter(value -> !value.isEmpty()).sorted().toList();
        return values.equals(expected.stream().sorted().toList());
    }

    @Override
    public void setSearchable(String collectionName, Long organId, Long revisionId, boolean searchable) {
        try {
            String filter = "organ_id == " + organId + " && revision_id == " + revisionId;
            // Upsert requires the full non-nullable row; only flipping searchable is rejected by Milvus.
            var rows = queryRevision(collectionName, filter, FULL_ROW_FIELDS);
            List<JsonObject> changes = rows.stream()
                .map(row -> copyRow(row.getEntity(), searchable, null))
                .toList();
            if (changes.isEmpty()) {
                log.warn("setSearchable found no entities: collection={}, organId={}, revisionId={}, searchable={}",
                    collectionName, organId, revisionId, searchable);
                throw new BusinessException(KnowledgeErrorCode.MILVUS_INVALID_RESPONSE);
            }
            client().upsert(UpsertReq.builder().collectionName(collectionName).data(changes).build());
            flush(collectionName);
            if (!confirmSearchable(collectionName, filter, changes.size(), searchable)) {
                log.warn("setSearchable confirm failed after upsert+flush: collection={}, organId={}, revisionId={}, expectedSearchable={}, expectedCount={}",
                    collectionName, organId, revisionId, searchable, changes.size());
                throw new BusinessException(KnowledgeErrorCode.MILVUS_INVALID_RESPONSE);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(KnowledgeErrorCode.MILVUS_OPERATION_FAILED, exception);
        }
    }

    private boolean confirmSearchable(String collectionName, String filter, int expectedCount, boolean searchable) {
        for (int attempt = 1; attempt <= SEARCHABLE_CONFIRM_ATTEMPTS; attempt++) {
            var actual = queryRevision(collectionName, filter, List.of("chunk_id", "searchable"));
            long matched = actual.stream().filter(row -> asBoolean(row.getEntity().get("searchable")) == searchable).count();
            if (actual.size() == expectedCount && matched == expectedCount) {
                return true;
            }
            log.info("setSearchable confirm attempt {}/{}: collection={}, expectedCount={}, actualCount={}, matchedSearchable={}",
                attempt, SEARCHABLE_CONFIRM_ATTEMPTS, collectionName, expectedCount, actual.size(), matched);
            if (attempt < SEARCHABLE_CONFIRM_ATTEMPTS) {
                try {
                    Thread.sleep(50L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private List<QueryResp.QueryResult> queryRevision(
        String collectionName, String filter, List<String> outputFields) {
        return client().query(QueryReq.builder().collectionName(collectionName).filter(filter)
            .outputFields(outputFields)
            .consistencyLevel(ConsistencyLevel.STRONG)
            .limit(REVISION_QUERY_LIMIT)
            .build()).getQueryResults();
    }

    private void flush(String collectionName) {
        client().flush(FlushReq.builder().collectionNames(List.of(collectionName)).build());
    }

    @Override
    public void setVisibility(String collectionName, Long organId, Long revisionId, String visibility) {
        try {
            String filter = "organ_id == " + organId + " && revision_id == " + revisionId;
            var rows = queryRevision(collectionName, filter, FULL_ROW_FIELDS);
            List<JsonObject> changes = rows.stream()
                .map(row -> copyRow(row.getEntity(), asBoolean(row.getEntity().get("searchable")), visibility))
                .toList();
            if (changes.isEmpty()) {
                log.warn("setVisibility found no entities: collection={}, organId={}, revisionId={}, visibility={}",
                    collectionName, organId, revisionId, visibility);
                return;
            }
            client().upsert(UpsertReq.builder().collectionName(collectionName).data(changes).build());
            flush(collectionName);
            log.info("setVisibility upserted: collection={}, organId={}, revisionId={}, visibility={}, count={}",
                collectionName, organId, revisionId, visibility, changes.size());
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(KnowledgeErrorCode.MILVUS_OPERATION_FAILED, exception);
        }
    }

    private JsonObject copyRow(Map<String, Object> entity, boolean searchable, String visibilityOverride) {
        JsonObject row = new JsonObject();
        row.addProperty("chunk_id", asLong(entity.get("chunk_id")));
        row.addProperty("organ_id", asLong(entity.get("organ_id")));
        row.addProperty("knowledge_id", asLong(entity.get("knowledge_id")));
        row.addProperty("revision_id", asLong(entity.get("revision_id")));
        row.addProperty("searchable", searchable);
        String visibility = visibilityOverride != null
            ? visibilityOverride
            : String.valueOf(entity.getOrDefault("visibility", "PRIVATE"));
        if (visibility == null || "null".equals(visibility) || visibility.isBlank()) {
            visibility = "PRIVATE";
        }
        row.addProperty("visibility", visibility);
        row.addProperty("source_type", String.valueOf(entity.get("source_type")));
        row.addProperty("content", String.valueOf(entity.get("content")));
        row.addProperty("embedding_profile", String.valueOf(entity.get("embedding_profile")));
        Object titlePath = entity.get("title_path");
        if (titlePath != null) {
            row.addProperty("title_path", String.valueOf(titlePath));
        }
        Object pageStart = entity.get("page_start");
        if (pageStart != null) {
            row.addProperty("page_start", asInt(pageStart));
        }
        Object pageEnd = entity.get("page_end");
        if (pageEnd != null) {
            row.addProperty("page_end", asInt(pageEnd));
        }
        JsonArray tags = new JsonArray();
        Object rawTags = entity.get("tags");
        if (rawTags instanceof List<?> values) {
            values.forEach(value -> tags.add(String.valueOf(value)));
        }
        row.add("tags", tags);
        JsonArray vector = new JsonArray();
        Object rawVector = entity.get("content_vector");
        if (rawVector instanceof List<?> values) {
            for (Object value : values) {
                vector.add(asFloat(value));
            }
        } else if (rawVector instanceof float[] values) {
            for (float value : values) {
                vector.add(value);
            }
        } else {
            throw new BusinessException(KnowledgeErrorCode.MILVUS_INVALID_RESPONSE);
        }
        row.add("content_vector", vector);
        return row;
    }

    private static long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private static int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static float asFloat(Object value) {
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return Float.parseFloat(String.valueOf(value));
    }

    /** Accepts Boolean, 0/1 numbers, and common string forms from Milvus SDK result maps. */
    static boolean asBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return false;
        }
        if ("1".equals(text) || "true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text)) {
            return true;
        }
        if ("0".equals(text) || "false".equalsIgnoreCase(text) || "no".equalsIgnoreCase(text)) {
            return false;
        }
        return Boolean.parseBoolean(text);
    }

    @Override
    public List<MilvusSearchCandidate> search(String collectionName, Long organId, boolean includePublic,
                                               List<String> allTagCodes, List<String> anyTagCodes, float[] vector,
                                               int topK) {
        try {
            var response = client().search(SearchReq.builder().collectionName(collectionName)
                .annsField("content_vector").data(List.of(new FloatVec(vector))).topK(topK)
                .metricType(IndexParam.MetricType.COSINE)
                .filter(MilvusFilterBuilder.searchable(organId, includePublic, allTagCodes, anyTagCodes))
                .outputFields(List.of("chunk_id")).build());
            if (response.getSearchResults().isEmpty()) return List.of();
            return response.getSearchResults().getFirst().stream()
                .map(result -> new MilvusSearchCandidate(Long.parseLong(String.valueOf(result.getEntity().get("chunk_id"))), result.getScore()))
                .toList();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(KnowledgeErrorCode.MILVUS_OPERATION_FAILED, exception);
        }
    }

    private MilvusClientV2 client() {
        MilvusClientV2 current = client;
        if (current == null) synchronized (this) {
            if (client == null) client = new MilvusClientV2(ConnectConfig.builder().uri(properties.getUri()).token(properties.getToken()).build());
            current = client;
        }
        return current;
    }

    @Override
    public void validateSchema(String collectionName, int dimension) {
        try {
            var schema = client().describeCollection(DescribeCollectionReq.builder().collectionName(collectionName).build()).getCollectionSchema();
            boolean key = schema.getFieldSchemaList().stream().anyMatch(field -> "chunk_id".equals(field.getName()) && field.getIsPrimaryKey());
            boolean vector = schema.getFieldSchemaList().stream().anyMatch(field -> "content_vector".equals(field.getName()) && field.getDimension() == dimension);
            boolean visibility = schema.getFieldSchemaList().stream().anyMatch(field -> "visibility".equals(field.getName()));
            if (!key || !vector || !visibility) {
                throw new BusinessException(KnowledgeErrorCode.MILVUS_INVALID_RESPONSE);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(KnowledgeErrorCode.MILVUS_OPERATION_FAILED, exception);
        }
    }

    private JsonObject json(MilvusIndexEntity entity) {
        JsonObject row = new JsonObject();
        row.addProperty("chunk_id", entity.chunkId());
        row.addProperty("organ_id", entity.organId());
        row.addProperty("knowledge_id", entity.knowledgeId());
        row.addProperty("revision_id", entity.revisionId());
        row.addProperty("source_type", entity.sourceType());
        row.addProperty("content", entity.content());
        if (entity.titlePath() != null) {
            row.addProperty("title_path", entity.titlePath());
        }
        if (entity.pageStart() != null) {
            row.addProperty("page_start", entity.pageStart());
        }
        if (entity.pageEnd() != null) {
            row.addProperty("page_end", entity.pageEnd());
        }
        row.addProperty("embedding_profile", entity.embeddingProfile());
        row.addProperty("searchable", false);
        row.addProperty("visibility",
            entity.visibility() == null || entity.visibility().isBlank() ? "PRIVATE" : entity.visibility());
        JsonArray tags = new JsonArray();
        entity.tags().forEach(tags::add);
        row.add("tags", tags);
        JsonArray vector = new JsonArray();
        for (float value : entity.vector()) {
            vector.add(value);
        }
        row.add("content_vector", vector);
        return row;
    }
}
