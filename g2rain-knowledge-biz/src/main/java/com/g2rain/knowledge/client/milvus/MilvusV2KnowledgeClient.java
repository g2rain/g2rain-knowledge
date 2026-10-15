package com.g2rain.knowledge.client.milvus;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.collection.request.DescribeCollectionReq;
import org.springframework.stereotype.Component;

import java.util.List;

/** Milvus V2 staged writer. Collection schema is owned by deployment migration. */
@Component
public class MilvusV2KnowledgeClient implements MilvusKnowledgeClient {
    private final MilvusProperties properties;
    private volatile MilvusClientV2 client;

    public MilvusV2KnowledgeClient(MilvusProperties properties) { this.properties = properties; }

    @Override
    public void upsertStaged(String collectionName, List<MilvusIndexEntity> entities) {
        if (entities.isEmpty()) return;
        try {
            List<JsonObject> data = entities.stream().map(this::json).toList();
            client().upsert(UpsertReq.builder().collectionName(collectionName).data(data).build());
        } catch (RuntimeException exception) {
            throw new BusinessException(KnowledgeErrorCode.EMBEDDING_OPERATION_FAILED);
        }
    }

    @Override
    public long countRevision(String collectionName, Long organId, Long revisionId) {
        try {
            String filter = "organ_id == " + organId + " && revision_id == " + revisionId;
            return client().query(QueryReq.builder().collectionName(collectionName).filter(filter)
                .outputFields(List.of("chunk_id")).build()).getQueryResults().size();
        } catch (RuntimeException exception) {
            throw new BusinessException(KnowledgeErrorCode.EMBEDDING_OPERATION_FAILED);
        }
    }

    @Override
    public void verifyStaged(String collectionName, List<MilvusIndexEntity> expected) {
        if (expected.isEmpty()) return;
        Long organId = expected.getFirst().organId(); Long revisionId = expected.getFirst().revisionId();
        try {
            String filter = "organ_id == " + organId + " && revision_id == " + revisionId;
            var rows = client().query(QueryReq.builder().collectionName(collectionName).filter(filter)
                .outputFields(List.of("chunk_id", "organ_id", "revision_id", "tags", "embedding_profile", "searchable")).build()).getQueryResults();
            if (rows.size() != expected.size()) throw new BusinessException(KnowledgeErrorCode.EMBEDDING_INVALID_RESPONSE);
            // SDK result maps are normalized here by field name; each expected entity must match all filter-critical fields.
            for (MilvusIndexEntity entity : expected) {
                boolean matched = rows.stream().anyMatch(row -> String.valueOf(row.getEntity().get("chunk_id")).equals(String.valueOf(entity.chunkId()))
                    && String.valueOf(row.getEntity().get("organ_id")).equals(String.valueOf(entity.organId()))
                    && String.valueOf(row.getEntity().get("revision_id")).equals(String.valueOf(entity.revisionId()))
                    && String.valueOf(row.getEntity().get("embedding_profile")).equals(entity.embeddingProfile())
                    && sameTags(row.getEntity().get("tags"), entity.tags())
                    && Boolean.FALSE.equals(row.getEntity().get("searchable")));
                if (!matched) throw new BusinessException(KnowledgeErrorCode.EMBEDDING_INVALID_RESPONSE);
            }
        } catch (RuntimeException exception) { throw new BusinessException(KnowledgeErrorCode.EMBEDDING_OPERATION_FAILED); }
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
            var rows = client().query(QueryReq.builder().collectionName(collectionName).filter(filter)
                .outputFields(List.of("chunk_id")).build()).getQueryResults();
            List<JsonObject> changes = rows.stream().map(row -> {
                JsonObject value = new JsonObject();
                value.addProperty("chunk_id", Long.parseLong(String.valueOf(row.getEntity().get("chunk_id"))));
                value.addProperty("searchable", searchable);
                return value;
            }).toList();
            if (changes.isEmpty()) {
                throw new BusinessException(KnowledgeErrorCode.EMBEDDING_INVALID_RESPONSE);
            }
            client().upsert(UpsertReq.builder().collectionName(collectionName).data(changes).build());
            var actual = client().query(QueryReq.builder().collectionName(collectionName).filter(filter)
                .outputFields(List.of("chunk_id", "searchable")).build()).getQueryResults();
            boolean confirmed = actual.size() == changes.size() && actual.stream()
                .allMatch(row -> Boolean.parseBoolean(String.valueOf(row.getEntity().get("searchable"))) == searchable);
            if (!confirmed) throw new BusinessException(KnowledgeErrorCode.EMBEDDING_INVALID_RESPONSE);
        } catch (RuntimeException exception) {
            throw new BusinessException(KnowledgeErrorCode.EMBEDDING_OPERATION_FAILED);
        }
    }

    @Override
    public List<MilvusSearchCandidate> search(String collectionName, Long organId, List<String> allTagCodes,
                                               List<String> anyTagCodes, float[] vector, int topK) {
        try {
            var response = client().search(SearchReq.builder().collectionName(collectionName)
                .annsField("content_vector").data(List.of(new FloatVec(vector))).topK(topK)
                .metricType(IndexParam.MetricType.COSINE)
                .filter(MilvusFilterBuilder.searchable(organId, allTagCodes, anyTagCodes))
                .outputFields(List.of("chunk_id")).build());
            if (response.getSearchResults().isEmpty()) return List.of();
            return response.getSearchResults().getFirst().stream()
                .map(result -> new MilvusSearchCandidate(Long.parseLong(String.valueOf(result.getEntity().get("chunk_id"))), result.getScore()))
                .toList();
        } catch (RuntimeException exception) {
            throw new BusinessException(KnowledgeErrorCode.EMBEDDING_OPERATION_FAILED);
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
            if (!key || !vector) throw new BusinessException(KnowledgeErrorCode.EMBEDDING_INVALID_RESPONSE);
        } catch (RuntimeException exception) {
            throw new BusinessException(KnowledgeErrorCode.EMBEDDING_OPERATION_FAILED);
        }
    }

    private JsonObject json(MilvusIndexEntity entity) {
        JsonObject row = new JsonObject();
        row.addProperty("chunk_id", entity.chunkId()); row.addProperty("organ_id", entity.organId());
        row.addProperty("knowledge_id", entity.knowledgeId()); row.addProperty("revision_id", entity.revisionId());
        row.addProperty("source_type", entity.sourceType()); row.addProperty("content", entity.content());
        row.addProperty("title_path", entity.titlePath()); row.addProperty("page_start", entity.pageStart()); row.addProperty("page_end", entity.pageEnd());
        row.addProperty("embedding_profile", entity.embeddingProfile()); row.addProperty("searchable", false);
        JsonArray tags = new JsonArray(); entity.tags().forEach(tags::add); row.add("tags", tags);
        JsonArray vector = new JsonArray(); for (float value : entity.vector()) vector.add(value); row.add("content_vector", vector);
        return row;
    }
}
