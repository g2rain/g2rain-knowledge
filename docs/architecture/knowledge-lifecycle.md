# 知识生命周期与数据模型

本页是生命周期与存储布局的摘要；状态机、任务顺序和发布规则以 [知识库业务流程](business-flows.md) 与 [知识库领域规则](domain-rules.md) 为准，表职责以 [MySQL 数据模型](mysql-schema.md) 为准，向量索引以 [Milvus 向量索引设计](milvus-design.md) 为准。

## 生命周期

权威顺序为：**人工审核原文与标签 → 解析 / 切片 / Embedding / 索引 → 可发布 → 人工同步发布**。MySQL 始终是活动版本权威，Milvus `searchable` 是发布命令同步维护的派生投影。

```text
上传文件或编辑 FAQ
  → 创建草稿版本（DRAFT）
  → 写入受控本地存储
  → 进入 REVIEWING；运营校验原文并确认主题 tags/priority
  → 审核通过后由 PROCESS 任务调用 Docling、HybridChunker 与 Embedding API，并以 searchable=false 写入 Milvus
  → 索引对账完成，进入 READY_TO_PUBLISH
  → 运营人工发布：预启用目标 Entity，MySQL CAS 切换 active_revision_id，再关闭旧 Entity
```

`PROCESS` 失败的版本处于 `PROCESS_FAILED`（可改回 `DRAFT`），不得进入发布。审核拒绝回到 `DRAFT`；不得在 `REVIEWING` / `APPROVED` / `PROCESSING` / `READY_TO_PUBLISH` / `PUBLISHED` / `DEPRECATED` 上直接改内容。旧版本可留作审计与回滚，但不得出现在生产检索中。人工审核只针对源文件或 FAQ，不针对解析/切片/向量中间产物（见 [DEV-001](deviations.md)）。

## 本地存储布局

```text
{KNOWLEDGE_STORAGE_ROOT}/
  {organ_id}/{knowledge_id}/{revision_id}/
    source/{safe-file-name}
    parsed/document.json
    parsed/document.md
    parsed/chunks.json
```

MySQL 保存相对路径、SHA-256、文件大小和内容类型。用户输入不能直接参与路径拼接；移动、删除和回收须由受控标识解析。物理表和本地存储路径统一使用 G2rain 的 `organ_id` 作为机构/租户隔离键。

多实例部署时，同一服务副本必须挂载**同一持久化卷**（或等价共享存储）；不得假设本地临时盘可跨实例复用。滚动发布期间，正在解析或回收的路径只能由持有任务租约的 Worker 操作。

## 核心数据

| 概念 | 责任 |
| --- | --- |
| `knowledge_item` | 稳定知识主题，以 `organ_id + knowledge_key` 唯一 |
| `knowledge_tag` | 组织级扁平标签字典，`organ_id + tag_code` 唯一；`tag_name` 仅展示 |
| `knowledge_item` | 主题元数据，含 `priority` 与 `tags_json` |
| `knowledge_revision` | 文件或 FAQ 的版本、解析/索引状态与 `embedding_profile` |
| `knowledge_release` | 当前生产版本 `active_revision_id` |
| `knowledge_answer_template` | 机构级回复模板，按 `template_code` 供知识问答选择 |
| `knowledge_chunk` | 可重建切片清单：定位、页码、token、内容哈希与 Entity 主键 |
| `knowledge_feedback` | 低满意度、人工接管与人工修正反馈 |

可重建的 chunk 正文只存于本地解析产物与 Milvus Entity；MySQL `knowledge_chunk` 不保存正文，只维护来源定位、页码、token 数、内容哈希，且其 `id` 直接作为 Milvus `chunk_id`，用于发布校验、删除和引用追踪。

Milvus Entity Schema 见 [Milvus 向量索引设计](milvus-design.md)。首期不把 `knowledge_key` 或 `priority` 写入 Milvus；`priority` 仅在 MySQL 侧作为运营排序与同主题内展示优先级，不参与向量召回过滤。标签仅用于业务分类和检索筛选。
