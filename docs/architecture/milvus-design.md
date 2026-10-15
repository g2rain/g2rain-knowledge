# Milvus 向量库设计

## 1. 定位与边界

Milvus 只保存可重建的检索索引，不承担业务真相存储职责。知识库、文档、修订、标签、任务、发布指针和审计均以 MySQL 为准。

- MySQL 决定修订是否有效、是否已发布以及当前生效版本。
- Milvus 保存 chunk 向量及检索所需的最小标量字段。
- Milvus 数据异常时，可由 MySQL 与本地原始文件重新解析、向量化并建索引。
- 租户隔离必须依赖可信上下文中的 `organ_id`，标签不得承担隔离职责。

## 2. Collection 策略

同一 Collection 内的向量维度和度量类型必须一致。首期按 Embedding 配置建立 Collection，例如：

`g2rain_knowledge_chunks_qwen_4096_v1`

首期固定配置：

| 项目 | 取值 |
| --- | --- |
| 向量字段 | `content_vector` |
| 类型 | `FLOAT_VECTOR` |
| 维度 | `4096` |
| 距离度量 | `COSINE` |
| 向量索引 | `HNSW` |
| 主键 | `chunk_id`，`INT64`，`autoID=false` |

`chunk_id` 直接使用 MySQL `knowledge_chunk.id`，不再维护第二套向量记录 ID。Embedding 模型、维度或归一化规则不兼容时创建新 Collection，并通过 `embedding_profile` 进行迁移和回滚，禁止在原 Collection 内混用。

## 3. Entity Schema

Milvus 使用显式 Schema，关闭动态业务字段。业务扩展通过 MySQL 标签体系表达，不向向量库动态增加 `product`、`region` 等固定字段。

| 字段 | Milvus 类型 | 用途 |
| --- | --- | --- |
| `chunk_id` | `INT64`，Primary Key | 对应 `knowledge_chunk.id` |
| `organ_id` | `INT64` | 租户隔离，来自可信上下文 |
| `knowledge_id` | `INT64` | 知识条目定位 |
| `revision_id` | `INT64` | 修订定位和发布校验 |
| `tags` | `ARRAY<VARCHAR>`，`max_capacity=64`，元素 `max_length=64` | 版本已选 `tag_code` 过滤 |
| `searchable` | `BOOL` | 是否进入召回候选集 |
| `source_type` | `VARCHAR` | 来源类型 |
| `content` | `VARCHAR` | chunk 文本，`max_length=65535` |
| `title` | `VARCHAR` | 文档标题 |
| `title_path` | `JSON` | 标题层级路径 |
| `page_start` | `INT32` | 起始页 |
| `page_end` | `INT32` | 结束页 |
| `embedding_profile` | `VARCHAR` | 模型、维度和归一化配置标识 |
| `schema_version` | `INT16` | Entity Schema 版本 |
| `content_vector` | `FLOAT_VECTOR(4096)` | 内容向量 |

写入前必须校验 VARCHAR 字节长度；超长 chunk 应重新切分或拒绝索引，不能静默截断。

## 4. 索引设计

- `content_vector`：HNSW + COSINE。
- `organ_id`、`knowledge_id`、`revision_id`、`searchable`、`embedding_profile`：建立标量 `AUTOINDEX`。
- `tags`：建立 `INVERTED` 标量索引，支持 ARRAY 操作符过滤。
- `M`、`efConstruction`、查询 `ef` 和召回数量由压测结果确定，不在首版文档中凭经验写死。

## 5. 标签过滤与检索

所有检索必须先拼接系统过滤条件：

```text
organ_id == <trusted_organ_id> && searchable == true
```

标签匹配语义：

```text
# AND：必须同时具备全部 tag_code
ARRAY_CONTAINS_ALL(tags, ["after_sales", "x100"])

# OR：具备任一 tag_code
ARRAY_CONTAINS_ANY(tags, ["cloud_line", "overseas_line"])
```

多个标签组之间统一使用 `AND` 组合。过滤值必须是已校验的 `tag_code`，并做表达式转义；禁止直接拼接未经处理的用户输入，也不得用展示名过滤。

Milvus 返回候选后，服务必须再以 MySQL 当前发布指针校验 `revision_id`，随后进行 rerank、阈值过滤和引用组装。这样即使发布切换后的旧 Entity 尚未关闭，也不会把旧版本交给 Agent Runtime。

## 6. 写入和幂等

`INDEX` 任务包含 Embedding 与 Milvus 写入：

1. 从 MySQL 读取已审核修订及其 chunk。
2. 调用外部 Embedding 接口，校验返回维度为 4096。
3. 以 `chunk_id` 为主键执行 upsert；重复执行覆盖同一 Entity。
4. 对账 chunk 数、Entity 数和抽样内容。
5. 成功后将修订推进到 `READY_TO_PUBLISH`；失败保留任务错误并允许重试。

标签变化会创建新的 `INDEX` 任务，重新 upsert 对应修订的 Entity。删除索引使用 `chunk_id` 或 `revision_id + organ_id` 过滤，并校验受影响范围。

## 7. 人工发布一致性

首期仅提供人工发布、人工回滚和人工下线，不创建发版任务。

人工发布顺序：

1. 校验目标修订为 `READY_TO_PUBLISH`，且 Milvus Entity 已完成对账。
2. 在 MySQL 事务外用 partial upsert 将目标修订 Entity 的 `searchable` 设为 `true`，请求只携带 `chunk_id` 与 `searchable`。
3. 使用 CAS 更新 MySQL 当前发布指针和目标、旧版本状态。
4. CAS 失败时，补偿关闭目标修订的 `searchable`。
5. CAS 成功后，同步用 partial upsert 关闭旧修订 Entity 的 `searchable`。
6. 旧修订关闭失败时告警并由管理员执行同步索引修复；检索侧仍以 MySQL 当前发布指针兜底。

## 8. 单机部署

首期在 `g2rain-deploy` 中以 Milvus Standalone 单容器运行：

- 使用嵌入式 etcd。
- 使用 Woodpecker 本地 WAL。
- 使用 Milvus 本地持久化存储。
- 不引入 MinIO、OSS 或外部消息队列。
- 挂载 `./data/milvus:/var/lib/milvus`，并挂载独立配置文件。
- 仅加入内部 Docker 网络，不映射宿主机 `19530`、`9091` 和 `2379` 端口。
- 开启鉴权，首次部署后修改默认 root 密码并创建最小权限服务账号；连接凭证通过 Secret 注入。

该模式满足首期简化部署目标，但不是高可用方案。容量、可用性或恢复时间目标提升后，应迁移到 Milvus Cluster 和独立对象存储，而不是继续纵向扩大单机容器。

Java 服务使用 Milvus 官方 Java SDK V2 Client，SDK 版本由项目依赖管理统一锁定。连接地址为 `http://milvus:19530`，认证配置名统一为 `MILVUS_TOKEN`。

## 9. 重建、备份与可观测性

以下变化需要新 Collection 或全量重建：

- Embedding 模型、维度、归一化规则变化。
- chunk 内容或切片算法变化。
- Entity Schema 不兼容变化。

Milvus 本地数据目录必须纳入部署备份，但备份不能替代重建能力。至少监控：

- INDEX 任务积压、失败率和重试次数。
- Embedding 延迟、限流和维度异常。
- Milvus 写入、查询延迟和错误率。
- MySQL chunk 数与 Milvus Entity 数差异。
- 当前发布修订的可检索 Entity 覆盖率。
- 容器磁盘、水位、健康检查和恢复演练结果。
