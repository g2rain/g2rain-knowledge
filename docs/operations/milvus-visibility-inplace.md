# Milvus visibility 原地加字段（RQ-008）

面向 **已有** `g2rain-deploy` 栈中的 knowledge collection。新建空库应直接使用已含 `visibility` 的 `config/milvus/knowledge-collection.json`；`init-collection.sh` 对**已存在**的同名 collection **不会**改 schema，存量升级必须手工加字段。

| 项 | 值（默认） |
| --- | --- |
| 容器 | `milvus-standalone`（Compose service 名 `milvus`） |
| 网络 | `g2rain-network` |
| Collection | `KNOWLEDGE_MILVUS_COLLECTION` → `g2rain_knowledge_chunks_qwen_4096_v1` |
| Token | `MILVUS_BOOTSTRAP_TOKEN` → `root:Milvus`（改密后以 `.env` 为准） |
| REST | 容器内 `http://127.0.0.1:19530` |

## 上线顺序

1. MySQL 执行 `scripts/migrations/2026-10-27-rq008-visibility.sql`（存量默认 `PRIVATE`）。
2. 本页：Milvus 原地加 `visibility` → 建索引 → load → 抽样确认。
3. 停用会写出无 `visibility` Entity 的旧 knowledge 实例，再发新后端与 knowledge-app。
4. 开放公共列表 / `includePublic` 前确认 MySQL 与 Milvus 标量一致。

## 加字段（g2rain-deploy）

请求体必须带嵌套 `schema`（扁平 `fieldName` 会报 `Schema` required）。

可控测试环境可把 `defaultValue` 设为 `PUBLIC`，与 MySQL 整表公开策略一致；正式默认建议 `PRIVATE`，再按 MySQL 对账。

```bash
docker exec milvus-standalone sh -lc '
TOKEN="${MILVUS_BOOTSTRAP_TOKEN:-root:Milvus}"
COLLECTION="${KNOWLEDGE_MILVUS_COLLECTION:-g2rain_knowledge_chunks_qwen_4096_v1}"
curl -sS -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" \
  --data "{
    \"collectionName\": \"${COLLECTION}\",
    \"schema\": {
      \"fieldName\": \"visibility\",
      \"dataType\": \"VarChar\",
      \"elementTypeParams\": {\"max_length\": \"16\"},
      \"nullable\": true,
      \"defaultValue\": \"PUBLIC\"
    }
  }" \
  http://127.0.0.1:19530/v2/vectordb/collections/fields/add
'
```

成功响应：`{"code":0,"data":{}}`。

## 索引与 load

```bash
docker exec milvus-standalone sh -lc '
TOKEN="${MILVUS_BOOTSTRAP_TOKEN:-root:Milvus}"
COLLECTION="${KNOWLEDGE_MILVUS_COLLECTION:-g2rain_knowledge_chunks_qwen_4096_v1}"
curl -sS -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" \
  --data "{
    \"collectionName\": \"${COLLECTION}\",
    \"indexParams\": [{
      \"fieldName\": \"visibility\",
      \"indexName\": \"visibility\",
      \"indexType\": \"AUTOINDEX\"
    }]
  }" \
  http://127.0.0.1:19530/v2/vectordb/indexes/create
'

docker exec milvus-standalone sh -lc '
TOKEN="${MILVUS_BOOTSTRAP_TOKEN:-root:Milvus}"
COLLECTION="${KNOWLEDGE_MILVUS_COLLECTION:-g2rain_knowledge_chunks_qwen_4096_v1}"
curl -sS -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" \
  --data "{\"collectionName\":\"${COLLECTION}\"}" \
  http://127.0.0.1:19530/v2/vectordb/collections/load
'
```

## 抽样确认

```bash
docker exec milvus-standalone sh -lc '
TOKEN="${MILVUS_BOOTSTRAP_TOKEN:-root:Milvus}"
COLLECTION="${KNOWLEDGE_MILVUS_COLLECTION:-g2rain_knowledge_chunks_qwen_4096_v1}"
curl -sS -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" \
  --data "{
    \"collectionName\": \"${COLLECTION}\",
    \"outputFields\": [\"chunk_id\", \"organ_id\", \"visibility\"],
    \"limit\": 5
  }" \
  http://127.0.0.1:19530/v2/vectordb/entities/query
'
```

若加字段时 `defaultValue` 已是目标值（如 `PUBLIC`），且与 MySQL 权威一致，**不必**再整库 upsert。仅当默认与 MySQL 不一致、或抽样仍为旧值时，再按主题活动 revision 对账写回（应用 `setVisibility` / 同步服务，或运维 upsert）。

## MySQL 侧（权威）

迁移后按环境决定是否整表公开（仅数据可控环境）：

```sql
UPDATE knowledge_item
SET visibility = 'PUBLIC', update_time = NOW()
WHERE visibility <> 'PUBLIC' OR visibility IS NULL;
```

正式环境应按主题白名单更新，不要默认整表 `PUBLIC`。

## 回退

先关公共读入口与共享范围变更入口，再回退 knowledge / app 镜像；MySQL 列与 Milvus 字段可保留。
