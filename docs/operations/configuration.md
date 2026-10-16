# 配置

实现以 [application.yml](../../g2rain-knowledge-startup/src/main/resources/application.yml) 为准。下表为文档名与实际环境变量对照。

| 文档/规划名 | 实际环境变量 | 目的 | 安全要求 |
| --- | --- | --- | --- |
| `KNOWLEDGE_STORAGE_ROOT` | `KNOWLEDGE_STORAGE_ROOT` | 持久化文件根目录 | 只允许受控挂载路径 |
| `DOCLING_BASE_URL` | `DOCLING_BASE_URL`（及可选 API Key） | Docling 内部服务 | 仅内部网络 |
| `MILVUS_URL` | **`MILVUS_URI`** | Milvus 地址 | 仅内部网络 |
| `MILVUS_TOKEN` | `MILVUS_TOKEN` | Milvus 凭据 | Secret 注入 |
| `MILVUS_COLLECTION` | **`KNOWLEDGE_MILVUS_COLLECTION`** | 当前 profile 的 collection | 不得由客户端指定 |
| `MYSQL_HOST` / `MYSQL_PORT` | 由 Spring/Nacos 数据源配置 | 元数据库 | 凭据 Secret 注入 |
| `EMBEDDING_URL` | Nacos `g2rain.knowledge.embedding.url` | 向量化完整地址，例如 `http://g2rain-model-nexus:8080/v1/embeddings` | 不记录请求原文；透传 PrincipalHeaders |
| `EMBEDDING_MODEL` | Nacos `g2rain.knowledge.embedding.model-name` | 向量模型名 | — |
| `EMBEDDING_DIMENSION` | **`KNOWLEDGE_EMBEDDING_DIMENSION`**（默认 4096） | 向量维度 | 须与 collection 一致 |
| `EMBEDDING_PROFILE` / `EMBEDDING_MODEL_VERSION` | 由 model+dimension 等组合表达；无独立 env | profile 绑定 | 变更视为新 profile |
| `RETRIEVE_TOP_K_MAX` | **`KNOWLEDGE_RETRIEVE_MAX_TOP_K`** | 最大 topK | — |
| （默认 topK） | `KNOWLEDGE_RETRIEVE_DEFAULT_TOP_K` | 默认 topK | — |
| `RETRIEVE_TOTAL_TIMEOUT_MS` | **`KNOWLEDGE_RETRIEVE_TOTAL_TIMEOUT_MS`** | 检索端到端预算 | 超时→依赖不可用/503 |
| `KNOWLEDGE_RERANK_ENABLED` | Nacos `g2rain.knowledge.rerank.enabled`（默认 true） | 是否调用重排序接口 | false 时保留 MySQL 复核后的原顺序并截取 topK，不调用模型 |
| `RERANK_*` | Nacos `g2rain.knowledge.rerank.url` / `model-name` | 重排完整地址，例如 `http://g2rain-model-nexus:8080/v1/rerank` | 仅 enabled 时调用；故障仍返回 503，不降级为无证据 |
| Answer | Nacos `g2rain.knowledge.answer.url` / `model-name` | 问答完整地址，例如 `http://g2rain-model-nexus:8080/v1/chat/completions` | 客户端不得指定模型 |

配置前缀为 `g2rain.knowledge.*`。生产环境缺少关键依赖、Secret 或持久化挂载时应快速失败。
